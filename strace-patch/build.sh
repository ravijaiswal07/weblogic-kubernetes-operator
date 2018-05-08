# Build a WL image that contains /usr/bin/strace
cp `which strace` .
docker build -t store/oracle/weblogic:12.2.1.3-strace .
