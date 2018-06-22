#!/bin/sh
# *************************************************************************
# This script supports coordinating the update of the MW_HOME directory
# based on an archive file prepared using copyBinary.sh
# 
# First the user will clone their gold master MW_HOME and then apply a patch
# allowing for testing and validation.  Then the user will perform
# copyBinary.sh on this patched MW_HOME and will distribute the resulting
# archive file to each remote node.  Finally, the RolloutService will
# coordinate actions on each node in order to shutdown the appropriate
# processes, apply the new patched MW_HOME into place, and eventually
# restart the processes.
#
# *************************************************************************
#
# Important internal variables:
#  MW_HOME      - This value is in the environment from the process that is
#                 performing the execution. 
#  BACKUP_DIR   - This indicates the location where the existing OracleHome
#                 should be moved to.  It could be later restored in the
#                 event of a rollback/revert.
#  PATCHED      - This indicates the location of the patch archive which
#                 will be applied using pasteBinary, or the patch dir which
#                 will be applied using a simple file move operation
#  ACTION       - This value should be one of the following:
#                prepare
#                update
#                validate
#                checkreq
#  REVERT_FROM_ERROR - This will indicate that this opertion is a revert
#			and this will require logic to determine whether to
#			keep the existing MW_HOME in the BACKUP_DIR or
#			to delete it when BACKUP_DIR is an archive file.
#  NEW_JAVA_HOME - This value will indicate that a new JAVA_HOME should be
#           applied to both the new MW_HOME as well as the particular domain
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
    msg="<`date  "+%Y-%m-%d%t%Z%t%H:%M:%S"`> <$msgType> <UpdateOracleHome> <$@>"
    echo $msg
}

info() {
    _msg Info "$@"
}

error() {
    _msg Error "$@"
}

updateError() {
    myerr="$( _msg Error "$@" )"
    echo $myerr
    echo $myerr >> $ERRFILE 
}


validateArguments() {
    if [ "$VERBOSE" ]; then
        info "Checking values: PATCHED: $PATCHED, BACKUP_DIR: $BACKUP_DIR, and NEW_JAVA_HOME: $NEW_JAVA_HOME"
    fi

    #TODO: what if NEW_JAVA_HOME == JAVA_HOME
    #we don't know wether our JAVA_HOME is universal, so proceed
    #later we can account for it

    # NEW_JAVA_HOME is allowed without PATCHED
    # BACKUP is still required for NEW_JAVA_HOME so we can rollback
    # first determine what might be required then validate items as a set
    #TODO: can use -z for not set var
    #TODO: can use : ${STATE?"Need to set STATE"}
    # : ${DEST:?"Need to set DEST non-empty"}

    if [ -z $PATCHED ] && [ -z $NEW_JAVA_HOME ]; then
        error "Must supply either PATCHED to update MW_HOME or NEW_JAVA_HOME to update JAVA_HOME"
        $EXIT 1
    fi

    if [ "$PATCHED" != "" ]; then

      #This is for relative paths
      if [ "${PATCHED:0:1}" != "/" ]; then
          PATCHED="$PWD"/"$PATCHED"
          tmp="$(cd $(dirname $PATCHED); pwd)"
          tmp_name="$(basename $PATCHED)"
          PATCHED="$tmp/$tmp_name"
      fi

      # $PATCHED should be a file OR it should be a dir
      if [ ! -d "$PATCHED" ] && [ ! -f "$PATCHED" ]; then
          error "PATCHED does not exist: $PATCHED, it must be set to a valid archive or directory"
          $EXIT 2
      fi

      # TODO: can we validate that $PATCHED is a valid OracleHome dir?
      if [ -d "$PATCHED" ] && [ ! -f "$PATCHED/wlserver/server/bin/startNodeManager.sh" ]; then
          error "PATCHED is a directory but does not seem to be a valid OracleHome, it is missing wlserver/server/bin/startNodeManager.sh"
          $EXIT 3
      fi

      # we have a valid PATCHED value, so validate DOMAIN_DIR is not under MW_HOME
      fullMW_HOME="$(cd "$MW_HOME"; pwd)"
      if [ "${DOMAIN_DIR##$fullMW_HOME}" != "${DOMAIN_DIR}" ]; then
        error "The domain directory $DOMAIN_DIR is a directory under the OracleHome directory, $fullMW_HOME, and this is an invalid topology for ZDT Control"
        $EXIT 3
      fi

      if [ "${PATCHED##$fullMW_HOME}" != "${PATCHED}" ]; then
        error "The UpdateOracleHome value $PATCHED is under the OracleHome directory, $fullMW_HOME, and this should be moved to be accessible outside of MW_HOME"
        $EXIT 3
      fi


    fi

    if [ "$NEW_JAVA_HOME" ]; then
      if [ ! -d "$NEW_JAVA_HOME" ]; then
        error "NEW_JAVA_HOME should be a valid directory: $NEW_JAVA_HOME"
        $EXIT 4
      fi
      checkNEW_JAVA_HOME="$(cd "$NEW_JAVA_HOME"; pwd)"
      checkMW_HOME="$(cd "$MW_HOME"; pwd)"
      if [ "${checkNEW_JAVA_HOME}" = "${checkMW_HOME}" ]; then
        error "The RolloutJavaHome value $NEW_JAVA_HOME is the same as the OracleHome directory, $MW_HOME.  This is likley a typo and a valid Java directory should be given."
        $EXIT 4
      fi
    fi

    #TODO: simplify checks
    if [ "$TMP_UPDATE_SCRIPT" = "" ]; then
      error "The $MW_HOME/wlserver/server/bin/startNodeManager.sh script supplies a value for TMP_UPDATE_SCRIPT, but this value is not defined now.  This is likely due to an invalid ZDT topology with a custom NodeManager.  Please make sure the custom NodeManager delegates to the WL_HOME/server/bin/startNodeManager.sh script."
      $EXIT 5
    fi
    fullMW_HOME="$(cd "$MW_HOME"; pwd)"
    if [ "${TMP_UPDATE_SCRIPT##$fullMW_HOME}" != "${TMP_UPDATE_SCRIPT}" ]; then
      error "The TMP_UPDATE_SCRIPT value $TMP_UPDATE_SCRIPT is under the OracleHome directory, $fullMW_HOME, and this should be moved to be accessible outside of MW_HOME.  This is likely due to an improper override of this environment value when starting the NodeManager."
      $EXIT 5
    fi

    # BACKUPDIR must be specified
    if [ "$BACKUP_DIR" = "" ]; then
      if [ "$PATCHED" != "" ]; then
          error "Must supply BACKUP_DIR"
          $EXIT 6
      fi
    else
       #This is for relative paths
        if [ "${BACKUP_DIR:0:1}" != "/" ]; then
            BACKUP_DIR="$PWD"/"$BACKUP_DIR"
            tmp="$(cd $(dirname $BACKUP_DIR); pwd)"
            tmp_name="$(basename $BACKUP_DIR)"
            BACKUP_DIR="$tmp/$tmp_name"
        fi

        fullMW_HOME="$(cd "$MW_HOME"; pwd)"
        checkBACKUP_DIR="$(cd "$BACKUP_DIR"; pwd)"
        if [ "${checkBACKUP_DIR}" = "${fullMW_HOME}" ]; then
            error "The BackupOracleHome value $BACKUP_DIR is the same as the OracleHome directory, $MW_HOME.  A unique BackupOracleHome dir should be given in order to move the MW_HOME aside for preservation."
            $EXIT 6
        fi
        if [ "${BACKUP_DIR##$fullMW_HOME}" != "${BACKUP_DIR}" ]; then
            error "The BackupOracleHome value $BACKUP_DIR is under the OracleHome directory, $fullMW_HOME, and this should be moved to be stored outside of MW_HOME"
            $EXIT 6
        fi


    fi


    if [ "$VERBOSE" ]; then
        info "Patched dir will be: $PATCHED"
        info "Backup dir will be: $BACKUP_DIR"
        info "Action is: $ACTION"
    fi  
}



establishINVLoc() {
    INV_DIR="$(cd $MW_HOME/../; pwd)"
    ORA_INV="$INV_DIR"/oraInventory_tmp
    ORA_LOC="$INV_DIR"/oraInst_tmp.loc
}

prepareUpdateScript() {
    validateArguments
    cleanup

    echo "#!/bin/sh
PATCHED="$PATCHED"
BACKUP_DIR="$BACKUP_DIR"
REVERT_FROM_ERROR=$REVERT_FROM_ERROR
ACTION=update
NEW_JAVA_HOME="$NEW_JAVA_HOME"
VERBOSE=$VERBOSE
export PATCHED
export BACKUP_DIR
export ACTION
export REVERT_FROM_ERROR
export NEW_JAVA_HOME
export VERBOSE
"$SCRIPTNAME" > "$VERBOSE_OUTFILE" 2>&1

if [ \$? -eq 0 ]; then
  cat "$VERBOSE_OUTFILE"
  if [ -n \"\$NEW_JAVA_HOME\" ]; then
    echo \"Setting JAVA_HOME to $NEW_JAVA_HOME\"
    JAVA_HOME=$NEW_JAVA_HOME
    echo \"JAVA_HOME is now \$JAVA_HOME\"
    export JAVA_HOME
    unset NEW_JAVA_HOME
    unset CLASSPATH
    #TODO: how to communicate with parent to reset JAVA_HOME?
    $EXIT 42
    #TODO: remove JAVA_HOME from path
  elif [ -d \"${MW_HOME}/${PATCH_BACKUP_DOM}\" ]; then
    echo \"Unsetting JAVA_HOME because we are rolling back to a previous JAVA ver\"
    unset JAVA_HOME
    unset CLASSPATH
    #TODO: remove JAVA_HOME from path
    $EXIT 42
  fi
else
  cat "$VERBOSE_OUTFILE"
  echo \"FAILURE\"
fi

" >> "$TMP_UPDATE_SCRIPT"

    #check error
    if [ $? -eq 0 ]; then  
        if [ "$VERBOSE" ]; then
            info "Success writing $TMP_UPDATE_SCRIPT"
        fi
    else
        error "Could not write $TMP_UPDATE_SCRIPT"
        $EXIT 7
    fi

    chmod +x "$TMP_UPDATE_SCRIPT"
    #checkerror
    if [ $? -eq 0 ]; then  
        if [ "$VERBOSE" ]; then
            info "Success chmod $TMP_UPDATE_SCRIPT"
        fi
    else
        error "Could not chmod $TMP_UPDATE_SCRIPT, must be executable"
        if [ "$VERBOSE" ]; then
            info "Cleaning up $TMP_UPDATE_SCRIPT by removing it"
        fi
        #cleanup
        rm "$TMP_UPDATE_SCRIPT"
        $EXIT 8
    fi

    if [ "$NEW_JAVA_HOME" ]; then
        backupJavaHomeDomainFiles
    fi
    info "Successfully prepared $TMP_UPDATE_SCRIPT"
    $EXIT 0
}

backupJavaHomeFiles() {
  if [ "$VERBOSE" ]; then
    info "backupJavaHomeFiles"
  fi
  if [ ! -d "${MW_HOME}/${PATCH_BACKUP_OH}" ]; then
    mkdir -p "${MW_HOME}/${PATCH_BACKUP_OH}"
    if [ $? -eq 0 ]; then
      if [ "$VERBOSE" ]; then
        info "created ${MW_HOME}/${PATCH_BACKUP_OH} dir"
      fi
    else
      error "Could not write ${MW_HOME}/${PATCH_BACKUP_OH} dir"
      $EXIT 9
    fi
  fi
  cd $MW_HOME
  find . -name "*" |xargs grep -sl $JAVA_HOME | cpio -pdm --unconditional ${MW_HOME}/${PATCH_BACKUP_OH}
  cd -
}


restoreJavaHomeFiles() {
  if [ "$VERBOSE" ]; then
    info "restoreJavaHomeFiles"
  fi
  cd ${MW_HOME}/${PATCH_BACKUP_OH}
  find . -name "*" |xargs grep -sl $JAVA_HOME | cpio -pdm --unconditional $MW_HOME
  cd -
  rm -fr ${MW_HOME}/${PATCH_BACKUP_OH}
}

backupJavaHomeDomainFiles() {
  if [ "$VERBOSE" ]; then
    info "backupJavaHomeDomainFiles"
  fi
  # backup copies of domain files
  # backup copy of domainDir/bin/setDomainEnv.sh
  if [ ! -d "${MW_HOME}/${PATCH_BACKUP_DOM}" ]; then
    mkdir -p "${MW_HOME}/${PATCH_BACKUP_DOM}"
    if [ $? -eq 0 ]; then
      if [ "$VERBOSE" ]; then
        info "created ${MW_HOME}/${PATCH_BACKUP_DOM} dir"
      fi
      else
        error "Could not write ${MW_HOME}/${PATCH_BACKUP_DOM} dir"
        $EXIT 9
      fi
  fi

  for filemapping in "${DOMAIN_FILES[@]}" ; do
    NAME=${filemappin%%:*}
    DOMAIN_PATH=${filemapping#*:}
    if [ -f "$DOMAIN_PATH" ]; then
      cp "$DOMAIN_PATH" "${MW_HOME}/${PATCH_BACKUP_DOM}/${NAME}"
      if [ $? -eq 0 ]; then
		    if [ "$VERBOSE" ]; then
		     info "copied $DOMAIN_PATH to ${MW_HOME}/${PATCH_BACKUP_DOM}/${NAME}"
	  	  fi
      else
		    error "Could not copy $DOMAIN_PATH to ${MW_HOME}/${PATCH_BACKUP_DOM}/${NAME}"
		    $EXIT 10
      fi
    fi
  done
}


deleteMW_HOME_FOR_REVERT() {
  if [ "$VERBOSE" ]; then
    info "PATCHED is an archive file so we simply remove MW_HOME"
  fi
  # this is an archive file, so we simply remove OHome
  rm -fr "$fullMW_HOME"
  if [ $? -eq 0 ]; then
	  if [ "$VERBOSE" ]; then
		  info "Successfully removed $fullMW_HOME for revert"
	  fi
	else
	  tmpMW_HOME=${fullMW_HOME}_tmp
	  warning "Error removing $MW_HOME for revert, moving to $tmpMW_HOME"
	  mv "$fullMW_HOME" "$tmpMW_HOME"
	  if [ $? -eq 0 ]; then
		  if [ "$VERBOSE" ]; then
		    info "Moved $fullMW_HOME to $tmpMW_HOME"
 		  fi
	  else
		  updateError "Could not move $fullMW_HOME to $tmpMW_HOME for removal during revert"
		  $EXIT 11
	  fi
	fi
}

##
# Usually we will be moving MW_HOME to BACKUP_DIR
# only when the BACKUP exists as an archive and we are REVERT_FROM_FALIURE
# then we will simply remove MW_HOME and move PATCHED into place
# all other cases we will overwrite $BACKUP_DIR with MW_HOME
##
backupMW_HOME() {
  if [ -f "$BACKUP_DIR" ] && [ "$REVERT_FROM_ERROR" ]; then
    deleteMW_HOME_FOR_REVERT
  else
    # no matter what we are going to delete BACKUP_DIR if it exists
	  if [ -f "$BACKUP_DIR" ] || [ -d "$BACKUP_DIR" ]; then
	    #TODO:REVIEW this, is this scenario possible?
	    if [ "$REVERT_FROM_ERROR" ]; then
		    warning "this is unexpected, during a revert the specified BACKUP_DIR $BACKUP_DIR already exists"
	    fi
	    rm -fr "$BACKUP_DIR"
	    if [ $? -eq 0 ]; then
        if [ "$VERBOSE" ]; then
          info "successfuly removed stale $BACKUP_DIR before backup"
        fi
      else
        updateError "Could not remove stale $BACKUP_DIR before backup"
        $EXIT 12
	    fi
	  fi
    #TODO: does this still happen after skipping this step for NEW_JAVA_HOME?
    # no patched copy to move into place, so simply copy our existing dir
    if [ -z $PATCHED ]; then
      if [ "$VERBOSE" ]; then
        info "Going to copy MW_HOME: $MW_HOME to BACKUP_DIR: $BACKUP_DIR"
      fi
      cp -fr $MW_HOME $BACKUP_DIR
      if [ $? -eq 0 ]; then
        if [ "$VERBOSE" ]; then
          info "Successfully copied $MW_HOME to $BACKUP_DIR"
        fi
      else
        updateError "Could not create a backup copy of $MW_HOME at $BACKUP_DIR"
        $EXIT 13
      fi
    else
	    mv "$fullMW_HOME" "$BACKUP_DIR"
	    if [ $? -eq 0 ]; then
	      if [ "VERBOSE" ]; then
		      info "Moved $fullMW_HOME to BACKUP: $BACKUP_DIR"
	      fi
	    else
	      updateError "Could not move $fullMW_HOME to backup: $BACKUP_DIR"
	      $EXIT 14
	    fi
    fi
  fi
}


##
# update phase
#
# first decide what operations we are going to do during udpate
# call detachHome if using pasteBinary.cmd
# then move aside existing MW_HOME
# when patched is a dir, we will copy it into place
# when patched is an archive, OR we have a new javahome then we will use pasteBin
##
updateOracleHome() {
  if [ "$VERBOSE" ]; then
	  info "UpdateOracleHome"
  fi

  #probably unnecessary since it is an internal call, but being safe here
  validateArguments
    
  #make sure we have the full MW_HOME value and not the
  #usual relative value: MW_HOME/wlserver/../
  fullMW_HOME="$(cd "$MW_HOME"; pwd)"
  

  PASTE_BIN_OPTIONS="-ohAlreadyCloned true"
  USE_JAVA="$JAVA_HOME"
  if [ -d "$NEW_JAVA_HOME" ]; then
    USE_PASTE_BIN="true"
    USE_JAVA="$NEW_JAVA_HOME"
  fi

  if [ -n "$PATCHED" ]; then
    if [ -d "$PATCHED" ]; then
      COPY_PATCHED="true"
    else
      USE_PASTE_BIN=true
      PASTE_BIN_OPTIONS="-archiveLoc $PATCHED"
    fi
  fi

  if [ "$USE_PASTE_BIN" ]; then
    # first detach, pasteBin will always reattach
    "$MW_HOME/oui/bin/detachHome.sh" > "$DETACH_HOME_OUT" 2>&1
    if [ $? -eq 0 ]; then
      if [ "$VERBOSE" ]; then
        info "Successfully detatched OracleHome"
      fi
    else

      updateError "Could not use detachHome.sh on $MW_HOME"
      cat "$DETACH_HOME_OUT" >> "$ERRFILE"
      $EXIT 15
    fi
  fi

  #should we validate java home update?
  if [ "$BACKUP_DIR" = "" ]; then
    #This means it is a JAVA_HOME update
    backupJavaHomeFiles
    KNOWN_LOCATION="$MW_HOME"
  else
    backupMW_HOME
    KNOWN_LOCATION="$BACKUP_DIR"


    if [ "$COPY_PATCHED" ]; then
	    if [ "$VERBOSE" ]; then
	      info "Now will move $PATCHED dir to $fullMW_HOME"
	    fi
	    mv "$PATCHED" "$fullMW_HOME"
	    #check error
	    if [ $? -eq 0 ]; then
	      if [ "$VERBOSE" ]; then
		      info "Successfully moved the PATCHED_DIR into place, $PATCHED -> $fullMW_HOME"
	      fi
	    else
	      updateError "Could not move $PATCHED to $fullMW_HOME, restoring $fullMW_HOME"
	      #should/could we attempt to detect processes?
	      #should we be careful and remove fullMW_HOME first if it exists?
	      mv "$BACKUP_DIR" "$fullMW_HOME"
	      $EXIT 16
	    fi
	  fi
  fi

  if [ "$USE_PASTE_BIN" ]; then

    if [ "$VERBOSE" ]; then
	    info "Now will use pasteBinary.sh with $PASTE_BIN_OPTIONS"
      info "calling $KNOWN_LOCATION/oracle_common/bin/pasteBinary.sh -javaHome $USE_JAVA -targetOracleHomeLoc $fullMW_HOME -ipl $KNOWN_LOCATION/oraInst.loc $PASTE_BIN_OPTIONS -executeSysPrereqs false"
	  fi

	  #pasteBinary
	  "${KNOWN_LOCATION}/oracle_common/bin/pasteBinary.sh" -javaHome "$USE_JAVA" -targetOracleHomeLoc "$fullMW_HOME" -ipl "$KNOWN_LOCATION/oraInst.loc" "$PASTE_BIN_OPTIONS" -executeSysPrereqs false > "$PASTE_BIN_OUT" 2>&1

	  #check error
	  if [ $? -eq 0 ]; then
	    if [ ! -f "${fullMW_HOME}/wlserver/server/bin/startNodeManager.sh" ]; then
	      warning "Possible pasteBin error, could not find ${fullMW_HOME}/wlserver/server/bin/startNodeManager.sh after successful operation"
	    fi
      if [ "$VERBOSE" ]; then
		    info "Successfully used pasteBinary to update $fullMW_HOME"
      fi
	  else
      updateError "Could not use pasteBinary.sh on $PATCHED, restoring $fullMW_HOME"
      cat "$PASTE_BIN_OUT" >> "$ERRFILE"

      #reattach MW_HOME
      "${KNOWN_LOCATION}/oui/bin/attachHome.sh" -invPtrLoc "$KNOWN_LOCATION/oraInst.loc" > "$ATTACH_HOME_OUT" 2>&1
      if [ $? -eq 0 ]; then
        if [ "$VERBOSE" ]; then
          info "Successfully reattached MW_HOME after error"
        fi
      else
        updateError "${KNOWN_LOCATION}/oui/bin/attachHome.sh reported error, $fullMW_HOME may not be registered"
        cat "$ATTACH_HOME_OUT" >> "$ERRFILE"
      fi
      if [ "$BACKUP_DIR" = "" ]; then
        restoreJavaHomeFiles
      else
        if [ -d "${fullMW_HOME}.err" ]; then
          rm -fr "$fullMW_HOME.err"
        fi
        mv "$fullMW_HOME" "${fullMW_HOME}.err"
        mv "$BACKUP_DIR" "$fullMW_HOME"
      fi
      $EXIT 17
	  fi
  fi

  updateJavaHomeFiles

  info "Successfully updated OracleHome"
  $EXIT 0
}

restoreFileFromPatchBackup() {
  if [ -f "${MW_HOME}/${PATCH_BACKUP_DOM}/${1}" ]; then
	  mv "${MW_HOME}/${PATCH_BACKUP_DOM}/${1}" "${2}"
	  if [ $? -eq 0 ]; then
	    if [ "$VERBOSE" ]; then
		    info "Successfully restored $2"
	    fi
	  else
	    updateError "Could not restore $2 from ${MW_HOME}/${PATCH_BACKUP_DOM}/${1}"
	    $EXIT 18
	  fi
  fi
}

replaceJavaHomeInFile() {
  if [ -f "$1" ]; then
	sed -e "s|$JAVA_HOME|$NEW_JAVA_HOME|" "$1" > "$1".new
	mv "$1".new "$1"
	if [ $? -eq 0 ]; then
	    if [ "$VERBOSE" ]; then
		info "sed executed successfully on $1 with $NEW_JAVA_HOME"
	    fi
	else
	    updateError "Could not update $1 with $NEW_JAVA_HOME"
	    recoverFromJavaHomeUpdateError
	    $EXIT 19
	fi

	#do some extra validation that the file does not contain $JAVA_HOME
	jcount="$(grep -c "$JAVA_HOME" "$1")"
	 if [ ! "$jcount" = "0" ]; then
	    updateError "$1 was not properly updated to remove $JAVA_HOME"
	    if [ $VERBOSE ]; then
		    cat "$1"
	    fi
	    recoverFromJavaHomeUpdateError
	    $EXIT 20
	 fi
  fi
}

restoreFilesFromPatchBackup() {
    # go through our list of files and put them back into place
    for filemapping in "${DOMAIN_FILES[@]}" ; do
        NAME="${filemapping%%:*}"
        DOMAIN_PATH="${filemapping#*:}"
        restoreFileFromPatchBackup "$NAME" "$DOMAIN_PATH"
    done
}

##
#
##
recoverFromJavaHomeUpdateError() {
  if [ "$BACKUP_DIR" = "" ]; then
    restoreJavaHomeFiles
  else
    if [ -d "${fullMW_HOME}.err" ]; then
      rm -fr "$fullMW_HOME.err"
    fi
    mv "$fullMW_HOME" "${fullMW_HOME}.err"
    mv "$BACKUP_DIR" "$fullMW_HOME"
  fi
  restoreFilesFromPatchBackup
}

##
# prefer the NEW_JAVA_HOME over any backup files
##
updateJavaHomeFiles() {
  #TODO: why bother checking if they are ==
  if [ -d "$NEW_JAVA_HOME" ] && [ ! "$NEW_JAVA_HOME" = "$JAVA_HOME" ]; then
  	# go through our list of files and replace JAVA_HOME
  	for filemapping in "${DOMAIN_FILES[@]}" ; do
              DOMAIN_PATH="${filemapping#*:}"
              replaceJavaHomeInFile "$DOMAIN_PATH"
  	done

  	# validate that domain/bin/setDomainEnv contains our new javahome
  	jcount="$(grep -c "$NEW_JAVA_HOME" "${DOMAIN_DIR}/bin/setDomainEnv.sh")"
  	if [ "$jcount" = "0" ]; then
  	    updateError "${DOMAIN_DIR}/bin/setDomainEnv.sh was not updated with $NEW_JAVA_HOME to replace $JAVA_HOME"
  	    cp "${DOMAIN_DIR}/bin/setDomainEnv.sh" "${DOMAIN_DIR}/bin/setDomainEnv.sh.tmp"
  	    recoverFromJavaHomeUpdateError
  	    $EXIT 21
  	fi

  	#update NM.properties under OH
  	replaceJavaHomeInFile "${MW_HOME}/oracle_common/common/nodemanager/nodemanager.properties"
  elif [ -f "${MW_HOME}/${PATCH_BACKUP_DOM}/setDomainEnv.sh" ] ; then
	  # if we have not done a JAVA_HOME update, and there is a PATCH_BACKUP that
	  # already exist - it is because because we must be doing a rollback - and
	  # we should restore the domain files
	  restoreFilesFromPatchBackup
  fi
}


validateUpdate() {
    # we know MW_HOME exists we are being exec from there
    # so just need to check backup dir and error file?
    # TODO: anything else we can do to validate
    #   - validate specific files in OracleHome?
    #   - if PATCHED is a dir then validate it is gone?
    #   - if PATCHED is a file should we delete it?
    #   - other?
    #   - existance of error file?
    if [ -f "$ERRFILE" ]; then
        error "Update FAILURE.  Attempting to show details from $ERRFILE"
        cat "$ERRFILE"
        $EXIT 22
    fi

    #TODO:
    #BACKUP_DIR should always exist
    # - mostly as dir
    # but when REVERT_FROM_ERROR it may exist as archive file

    info "Update Successful"
    cleanup
    $EXIT 0
}



checkPrereq() {
    if [ "$VERBOSE" ]; then
	    info "Checking PATCHED: $PATCHED"
    fi

    validateArguments

    info "checkreq Successful"
    $EXIT 0
}


# begin the basic outline of the script exec

# check mw_home always
if [ "$MW_HOME" = "" ]; then
    error "MW_HOME is not set.  Cannot proceed."
    $EXIT 23
fi

if [ ! -d "$MW_HOME" ]; then
    error "MW_HOME is not a directory: $MW_HOME.  Cannot proceed."
    $EXIT 24
fi

cleanup() {
 fileCleanup "$ERRFILE"
 fileCleanup "$TMP_UPDATE_SCRIPT"
 fileCleanup "$VERBOSE_OUTFILE"
 fileCleanup "$PASTE_BIN_OUT"
 fileCleanup "$DETACH_HOME_OUT"
 fileCleanup "$ATTACH_HOME_OUT"
 if [ "$VERBOSE" ]; then
   info "Files cleaned"
 fi
}

fileCleanup() {
  if [ -e $1 ]; then
	  rm -fr $1
    if [ $? -eq 0 ]; then
      if [ "$DEBUG" ]; then
        info "Successfully cleaned up $1"
      fi
    else
       warning "Unable to cleanup file $1"
    fi
  fi
}

updateOH() {
    mypwd="$(pwd)"
    # Determine the location of this script...
    # Note: this will not work if the script is sourced (. ./config.sh)
    SCRIPTNAME=$0
    case ${SCRIPTNAME} in
	/*)  SCRIPTPATH=`dirname "${SCRIPTNAME}"` ;;
	*)  SCRIPTPATH=`dirname "${mypwd}/${SCRIPTNAME}"` ;;
    esac
    ERRFILE=${SCRIPTPATH}/updateErrors.err
    VERBOSE_OUTFILE=${SCRIPTNAME}.out
    PASTE_BIN_OUT=${SCRIPTPATH}/pasteBin.out
    DETACH_HOME_OUT=${SCRIPTPATH}/detachHome.out
    ATTACH_HOME_OUT=${SCRIPTPATH}/attachHOme.out
    #This script lives in domain/bin/patching
    DOMAIN_DIR=${SCRIPTPATH}/../../
    #Patch backup dir for storing backup scripts
    PATCH_BACKUP=patching_backup
    PATCH_BACKUP_DOM=${PATCH_BACKUP}/domain
    PATCH_BACKUP_OH=${PATCH_BACKUP}/ohome
    DOMAIN_FILES=( "setDomainEnv.sh:${DOMAIN_DIR}/bin/setDomainEnv.sh"
                   "nodemanager.properties:${DOMAIN_DIR}/nodemanager/nodemanager.properties"
                   "tokenValue.properties:${DOMAIN_DIR}/init-info/tokenValue.properties"
                   "domain-info.xml:${DOMAIN_DIR}/init-info/domain-info.xml"
                   "nodemanager-properties.xml:${DOMAIN_DIR}/init-info/nodemanager-properties.xml"
                   "startscript.xml:${DOMAIN_DIR}/init-info/startscript.xml"
                   "setNMJavaHome.sh:${DOMAIN_DIR}/bin/setNMJavaHome.sh"
                 )

    if [ "$ACTION" = "" ]; then
	error "ACTION must be defined."
	$EXIT 25
    fi
    if [ "$ACTION" = "prepare" ]; then
	prepareUpdateScript
    elif [ "$ACTION" = "update" ]; then
	updateOracleHome
    elif [ "$ACTION" = "validate" ]; then
	validateUpdate
    elif [ "$ACTION" = "checkreq" ]; then
	checkPrereq
    else
	error "Fatal Error: $ACTION is not supported"
	$EXIT 26
    fi

}

updateOH


