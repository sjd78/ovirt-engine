#!/bin/bash 
#include db general functions
source ./dbfunctions.sh
source ./dbcustomfunctions.sh

#setting defaults
set_defaults

usage() {
    printf "Usage: ${ME} [-h] [-s SERVERNAME] [-d DATABASE] [-u USERNAME] [-v]\n"
    printf "\n"
    printf "\t-s SERVERNAME - The database servername for the database (def. ${SERVERNAME})\n"
    printf "\t-d DATABASE   - The database name                        (def. ${DATABASE})\n"
    printf "\t-u USERNAME   - The username for the database.\n"
    printf "\t-l LOGFILE    - The logfile for capturing output         (def. ${LOGFILE}\n"
    printf "\t-v            - Turn on verbosity (WARNING: lots of output)\n"
    printf "\t-h            - This help text.\n"
    printf "\n"

    exit 0
}

DEBUG () {
    if $VERBOSE; then
        printf "DEBUG: $*"
    fi
}

while getopts hs:d:u:p:l:v option; do
    case $option in
        s) SERVERNAME=$OPTARG;;
        d) DATABASE=$OPTARG;;
        u) USERNAME=$OPTARG;;
	l) LOGFILE=$OPTARG;;
        v) VERBOSE=true;;
        h) usage;;
    esac
done
if [ -e /usr/share/pgsql/contrib/uuid-ossp.sql ] ; then
# TODO fix the commandlines here
printf "Creating the database: ${DATABASE}\n"
#try to drop the database first (if exists)
dropdb --username=${USERNAME} --host=${SERVERNAME} ${DATABASE} -e > /dev/null
createdb --username=${USERNAME} --host=${SERVERNAME} ${DATABASE} -e -E UTF8 > /dev/null
if [ $? -ne 0 ]
    then
      printf "Failed to create database ${DATABASE}\n"
      exit 1;
fi
createlang --dbname=${DATABASE} --echo --username=${USERNAME} plpgsql >& /dev/null
#set database min error level
CMD="ALTER DATABASE ${DATABASE} SET client_min_messages=ERROR;"
execute_command "${CMD}"  ${DATABASE} > /dev/null
printf "Inserting UUID functions...\n"

echo user name is: ${USERNAME} 
psql -d ${DATABASE} -U ${USERNAME} -f /usr/share/pgsql/contrib/uuid-ossp.sql

printf "Creating tables...\n"
execute_file "create_tables.sql" ${DATABASE} > /dev/null

printf "Creating functions...\n"
execute_file "create_functions.sql" ${DATABASE} > /dev/null

printf "Creating common functions...\n"
execute_file "common_sp.sql" ${DATABASE} > /dev/null

#inserting initial data
insert_initial_data

# Running upgrade scripts
printf "Running upgrade scripts...\n"
run_upgrade_files

printf "Creating views...\n"
execute_file "create_views.sql" ${DATABASE} > /dev/null


printf "Creating stored procedures...\n"

for sql in $(ls *sp.sql); do
    printf "creating stored procedures from $sql ...\n"
    execute_file $sql ${DATABASE} > /dev/null
done


else 
  printf "\nThe file /usr/share/pgsql/contrib/uuid-ossp.sql does not exist. It is possible the postgresql-contrib package was not installed\n"
  printf "In order to install the package please perform:\n"
  printf "\nyum provides postgresql-contrib\nThis will determine which package should be installed. For example, for fedora 14 it should be: postgresql-contrib-8.4.7-1.fc14.x86_64\n"
  printf "\nyum install package-name\nFor example, for fedora 14 it should be: yum install postgresql-contrib-8.4.7-1.fc14.x86_64\n"
  printf "After installation is done, please run create_db.sh script again\n"
fi


exit $?
