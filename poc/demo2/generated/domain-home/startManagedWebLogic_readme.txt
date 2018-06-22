
To start managed servers, use either startManagedWebLogic.cmd
or startManagedWebLogic.sh, as appropriate for your environment.

Usage instructions:

   (1) If you are starting the managed server from a remote machine,
       use the Configuration Wizard to create a domain.  This will
       ensure that you have all files needed to start the server.

   (2) Navigate to your domain bin directory and execute the following:

   (3) For details about servers defined in domain, look in the Admin Console
       or in config/config.xml.

       startManagedWebLogic.sh "my_managed_server" "http://<administration_server_host_name>:7100"


For ease of reference, you defined the following managed
servers in your domain:

    ms1
    ms2
    ms3
