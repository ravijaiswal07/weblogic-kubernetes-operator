#!/bin/sh
# *************************************************************************
# This script supports coordinating the update of an Application source.
#
# First the user will update their applicacation and test it.  Then the user
# will distribute the resulting application source to each remote node.
# Finally, the RolloutService will coordinate actions on each node in order to
# shutdown the appropriate processes, apply the new patched application version
# into place, and eventually restart the processes with specific redeploy calls
# to trigger uptake of the app.
#
# The application source files will be preserved in the specified BACKUP_DIR,
# and then the patched application source will be moved into place to be picked
# up at the appropriate time.  If any part of this operation should fail, all
# steps will be reverted if possible.
#
# *************************************************************************
#
# Important internal variables:
#  CURRENT      - This indicates the current application location
#  BACKUP_DIR   - This indicates the location where the CURRENT location
#                 should be moved to.  It could be later restored in the
#                 event of a rollback/revert.
#  PATCHED      - This indicates the location of the patched app to move into
#                 the CURRENT location
#  ACTION       - This value should be one of the following:
#                update
#                checkreq
#  RESUME       - This indicates validation should exit gracefully in the instance
#                 the operation may have already been performed
#
#  VERBOSE       - This value indicates extra output should be enabled
#
# *************************************************************************
umask 027
EXIT=exit
OS="$(uname -sr)"

# In order to ensure that overall formatting and date formatting are
# consistent, all output should be funneled through here and this
# should only be called by functions such as 'info' and 'error'
# $1   - type of message (e.g. 'info')
# $2-N - message to output
_msg() {
    msgType="$1"
    shift
    # AIX/HPIA/Solaris 5.10 do not allow space in date format string
    msg="<`date  "+%Y-%m-%d%t%Z%t%H:%M:%S"`> <$msgType> <UpdateApplication> <$@>"
    echo $msg
}

info() {
    _msg Info "$@"
}

error() {
    _msg Error "$@"
}


validateArguments() {
    if [ "$VERBOSE" ]; then
        info "Checking values: CURRENT: $CURRENT, PATCHED: $PATCHED, and BACKUP_DIR: $BACKUP_DIR"
    fi

    if [ -z $CURRENT ]; then
       error "Must supply CURRENT to update the current location"
       $EXIT 1
    fi

    if [ -z $PATCHED ]; then
        error "Must supply PATCHED to update CURRENT"
        $EXIT 1
    fi

    #This is for relative paths
      if [ "${CURRENT:0:1}" != "/" ]; then
          #For external stage the relative path for current should be to the domain
          REL_CURRENT="$DOMAIN_DIR"/"$CURRENT"
          tmp="$(cd $(dirname $REL_CURRENT); pwd)"
          tmp_name="$(basename $REL_CURRENT)"
          REL_CURRENT="$tmp/$tmp_name"
          if [ "$VERBOSE" ]; then
              info "REL_CURRENT1: $REL_CURRENT"
          fi

          #If the path relative to the domain does not exist, then try relative to us
          if [ ! -d "$REL_CURRENT" ] && [ ! -f "$REL_CURRENT" ]; then
              REL1=$REL_CURRENT
              REL_CURRENT="$PWD"/"$CURRENT"
              tmp="$(cd $(dirname $REL_CURRENT); pwd)"
              tmp_name="$(basename $REL_CURRENT)"
              REL_CURRENT="$tmp/$tmp_name"
              if [ "$VERBOSE" ]; then
                  info "REL_CURRENT2: $REL_CURRENT"
              fi

              if [ ! -d "$REL_CURRENT" ] && [ ! -f "$REL_CURRENT" ]; then
                  error "Could not find the source relative to the domain or the domain/bin/patching dir: $CURRENT.  It must be set to a valid archive or directory"
                  $EXIT 1
              fi
         fi
         CURRENT=$REL_CURRENT
      fi

      # CURRENT should be a file OR it should be a dir
      if [ ! -d "$CURRENT" ] && [ ! -f "$CURRENT" ]; then
          error "CURRENT does not exist: $CURRENT, it must be set to a valid archive or directory"
          $EXIT 1
      fi


    #This is for relative paths
      if [ "${PATCHED:0:1}" != "/" ]; then
          #For external stage the relative path for current should be to the domain
          REL_PATCHED="$DOMAIN_DIR"/"$PATCHED"
          tmp="$(cd $(dirname $PATCHED); pwd)"
          tmp_name="$(basename $PATCHED)"
          PATCHED="$tmp/$tmp_name"
          #If the path relative to the domain does not exist, then try relative to us
          if [ ! -d "$REL_PATCHED" ] && [ ! -f "$REL_PATCHED" ]; then
              REL1=$REL_PATCHED
              REL_PATCHED="$PWD"/"$PATCHED"
              tmp="$(cd $(dirname $REL_PATCHED); pwd)"
              tmp_name="$(basename $REL_PATCHED)"
              REL_PATCHED="$tmp/$tmp_name"

              if [ ! -d "$REL_PATCHED" ] && [ ! -f "$REL_PATCHED" ]; then
                  error "Could not find the patched source relative to the domain or the domain/bin/patching dir: $PATCHED.  It must be set to a valid archive or directory"
                  $EXIT 1
              fi
         fi
         PATCHED=$REL_PATCHED
      fi

      # $PATCHED should be a file OR it should be a dir
      if [ ! -d "$PATCHED" ] && [ ! -f "$PATCHED" ]; then
          if [ -n "${RESUME}" ] && [ -n $BACKUP_DIR ] && [ -e "$BACKUP_DIR" ] ; then
           info "PATCHED does not exist after resume: $PATCHED.  It most likely has already been applied"
           $EXIT 0
          fi
          error "PATCHED does not exist: $PATCHED, it must be set to a valid archive or directory"
          $EXIT 1
      fi

      if [ -d "$PATCHED" ]; then
          checkPATCHED="$(cd $PATCHED; pwd)"
      else
          checkPATCHED=$PATCHED
      fi

      if [ -d "$CURRENT" ]; then
          checkCURRENT="$(cd $CURRENT; pwd)"
      else
          checkCURRENT=$CURRENT
      fi

      if [ "${checkPATCHED}" = "${checkCURRENT}" ]; then
        error "Application patchedLocation $PATCHED is the same as the current location, $CURRENT.  A unique patchedLocation path should be given in order to move the current location aside for preservation."
        $EXIT 1
      fi

    # BACKUPDIR must be specified
    if [ -z $BACKUP_DIR ]; then
        error "Must supply BACKUP_DIR"
        $EXIT 1
    fi

    #This is for relative paths
    if [ "${BACKUP_DIR:0:1}" != "/" ]; then
        #For external stage the relative path for current should be to the domain
        BACKUP_DIR="$DOMAIN_DIR"/"$BACKUP_DIR"
        #TODO: validate parent of BACkUP_DIR
        tmp="$(cd $(dirname $BACKUP_DIR); pwd)"

        #If the path relative to the domain does not exist, then try relative to us
        if [ ! -d "$tmp" ]; then
          REL1=$REL_BACKUP_DIR
          REL_BACKUP_DIR="$PWD"/"$BACKUP_DIR"
          tmp="$(cd $(dirname $REL_BACKUP_DIR); pwd)"
          
          if [ ! -d "$tmp" ]; then
              error "Could not find the backup dir relative to the domain at $REL1 or the current working dir at $REL_BACKUP_DIR.  The given value $BACKUP_DIR must be set to a valid archive or directory"
              $EXIT 1
          fi
        fi

        tmp_name="$(basename $BACKUP_DIR)"
        BACKUP_DIR="$tmp/$tmp_name"
    fi

    if [ -d "$BACKUP_DIR" ]; then
        checkBACKUP_DIR="$(cd $BACKUP_DIR; pwd)"
    else
        checkBACKUP_DIR=$BACKUP_DIR
    fi

    info "checkCURRENT: $checkCURRENT checkBACKUP_DIR: $checkBACKUP_DIR"
    if [ "${checkPATCHED}" = "${checkBACKUP_DIR}" ]; then
        error "Application backupLocation $BACKUP_DIR is the same location as the patchedLocation $PATCHED.  A unique backupLocation path should be given in order to move the current location aside for preservation."
        $EXIT 1
    fi

    if [ "${checkCURRENT}" = "${checkBACKUP_DIR}" ]; then
        error "Application backupLocation $BACKUP_DIR is the same as the current location, $CURRENT.  A unique backupLocation path should be given in order to move the current location aside for preservation."
        $EXIT 1
    fi
    if [ "$VERBOSE" ]; then
        info "Current will be: $CURRENT"
        info "Patched will be: $PATCHED"
        info "Backup dir will be: $BACKUP_DIR"
        info "Action is: $ACTION"
    fi
}



##
#
##
backupCurrent() {
  if [ "$VERBOSE" ]; then
	  info "backupCurrent"
  fi
  # no matter what we are going to delete BACKUP_DIR if it exists
	if [ -f "$BACKUP_DIR" ] || [ -d "$BACKUP_DIR" ]; then
	    rm -fr "$BACKUP_DIR"
	    if [ $? -eq 0 ]; then
        if [ "$VERBOSE" ]; then
          info "successfuly removed stale $BACKUP_DIR before backup"
        fi
      else
        error "Could not remove stale $BACKUP_DIR before backup"
        $EXIT 1
	    fi
	fi

  mv "$CURRENT" "$BACKUP_DIR"
	if [ $? -eq 0 ]; then
	  if [ "VERBOSE" ]; then
		  info "Moved $CURRENT to BACKUP: $BACKUP_DIR"
	  fi
	else
	  error "Could not move $CURRENT to backup: $BACKUP_DIR"
	  $EXIT 1
	fi
}


##
# update phase
#
##
updateCurrent() {
  if [ "$VERBOSE" ]; then
	  info "UpdateCurrent"
  fi

  validateArguments

  #backup
  backupCurrent

 if [ "$VERBOSE" ]; then
   info "Now will move $PATCHED dir to $CURRENT"
 fi
 cp -R "$PATCHED" "$CURRENT"
 #check error
 if [ $? -eq 0 ]; then
   if [ "$VERBOSE" ]; then
    info "Successfully moved the PATCHED_DIR into place, $PATCHED -> $CURRENT"
   fi
 else
   error "Could not move $PATCHED to $CURRENT, restoring $CURRENT"
   #should/could we attempt to detect processes?
   #should we be careful and remove CURRENT first if it exists?
	 mv "$BACKUP_DIR" "$CURRENT"
	 $EXIT 1
	fi

  touch "$CURRENT"
  info "Successfully updated $CURRENT"
  $EXIT 0
}


checkPrereq() {
    if [ "$VERBOSE" ]; then
	    info "Checking prereq arguments"
    fi

    validateArguments

    info "checkreq Successful"
    $EXIT 0
}


# begin the basic outline of the script exec
updateApp() {
    mypwd="$(pwd)"
    # Determine the location of this script...
    # Note: this will not work if the script is sourced (. ./config.sh)
    SCRIPTNAME=$0
    case ${SCRIPTNAME} in
	/*)  SCRIPTPATH=`dirname "${SCRIPTNAME}"` ;;
	*)  SCRIPTPATH=`dirname "${mypwd}/${SCRIPTNAME}"` ;;
    esac

    #This script lives in domain/bin/patching
    DOMAIN_DIR=${SCRIPTPATH}/../../

    if [ "$ACTION" = "" ]; then
	    error "ACTION must be defined."
	    $EXIT 1
    fi
    if [ "$ACTION" = "update" ]; then
	      updateCurrent
    elif [ "$ACTION" = "checkreq" ]; then
	      checkPrereq
    else
	      error "Fatal Error: $ACTION is not supported"
	      $EXIT 1
    fi

}

updateApp


