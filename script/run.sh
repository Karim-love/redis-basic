#!/bin/sh

###########################################
## ENV
###########################################
JAR="./redis-basic.jar"
JDK="../jdk"
MODULE_NAME="REDIS_BASIC"
NOW=$(date +"%F")
LOG_DIR=logs
LOG_FILE=server_${NOW}.log
OPT="-Xms1g -Xmx1g -Xss256k"
DUMP="-Dlogback.configurationFile=./logback.xml -XX:NewRatio=2 -XX:+DisableExplicitGC -Djava.awt.headless=true"
LOCATION=$(cd ./"$(dirname "$0")" && pwd)
STOP_PORT=

###########################################
# EXPORT PATH
###########################################
export LD_LIBRARY_PATH=.:./lib
export JAVA_HOME=${JDK}
export PATH=${JAVA_HOME}/bin:$PATH
export CLASS_PATH=.:./lib:./logback.xml
export LANG=ko_KR.UTF-8

###########################################
# JDK CHECK
###########################################
if ! test -d ${JDK} ; then
    echo " * Please Confirm Your Jdk Directory : ${JDK}"
    exit 1
fi
if ! test -d ${LOG_DIR} ; then
    mkdir ${LOG_DIR}
fi

###########################################
# PROC GREP PATTERN
###########################################
find_process () {
    PID_TEMP=`ps -ef | grep "${JDK}" | grep "${JAR}" | grep "${LOCATION}" | grep java | grep -v grep | awk '{ print $2 }'`
    if [ "x${PID_TEMP}" = "x" ]; then
        PID=-1
    else
        PID=${PID_TEMP}
    fi
}

###########################################
# PROC COUNT
###########################################
find_process_count () {
    PROC_COUNT=`ps -ef | grep "${JDK}" | grep "${JAR}" | grep "${LOCATION}" | grep java | grep -v grep | wc -l`
}

print_start () {
    echo ""
    echo " ============================================"
    echo " | ${MODULE_NAME} SCRIPT START"
    echo " ============================================"
    echo ""
}

print_end () {
    echo ""
    echo " ============================================"
    echo " | ${MODULE_NAME} SCRIPT END! BYE-BYE."
    echo " ============================================"
    echo ""
    exit 0
}


###########################################
# START METHOD
###########################################
start () {
    print_start
    find_process_count
    if [ ${PROC_COUNT} -gt 0 ]; then
      find_process
      echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} IS RUNNING STATE...  PID: ${PID}"
      print_end
    fi

    echo " * [$(date +"%F_%H:%M:%S")] : START PROCESS CALL. STDOUT TO ${LOG_DIR}/${LOG_FILE} file."
    if [ -e ${JAR} ]
    then
        JAVA_EXE=`which java`
        exec ${JAVA_EXE} ${OPT} ${DUMP} -jar ${JAR} -server -vmargs -Xverify:none -XX:+HeapDumpOnCtrlBreak location=${LOCATION} > ${LOG_DIR}/${LOG_FILE} 2>&1 &
    fi


    x=1
    while [ ${x} -lt 10 ]
    do
       find_process_count
       if [ ${PROC_COUNT} -gt 0 ]; then
         x=100
         find_process
         echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} IS STARTED... : ${PID}"
         echo " * [$(date +"%F_%H:%M:%S")] : PLEASE READ THE LOG FILE"
         echo " * [$(date +"%F_%H:%M:%S")] : TASK RESULT => SUCCESS"
         print_end
       fi
       x=$((x+1))
       sleep 1
    done

    echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} IS NOT STARTED OR ERROR."
    echo " * [$(date +"%F_%H:%M:%S")] : TASK RESULT => FAIL"
    echo " * [$(date +"%F_%H:%M:%S")] : PLEASE READ THE LOG FILE(${LOG_DIR}/${LOG_FILE})"

    print_end
}

###########################################
# END METHOD
###########################################
stopF () {

    print_start
    find_process_count
    echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} PROCESS COUNT : ${PROC_COUNT}"
    if [ ${PROC_COUNT} -lt 1 ]; then
      echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} IS NOT LOADED. THERE IS NO SENSE IN STOP."
      print_end
    fi

    find_process
    echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} RUNNING PROCESS PID : ${PID}"
    for JAR_PID in ${PID}
    do
       kill -9 ${JAR_PID}
       echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} STOP REQUEST : ${JAR_PID}"
       echo " * [$(date +"%F_%H:%M:%S")] : PLEASE WAIT FOR STOPPED. READ THE LOG FILE."
    done

    x=1
    while [ ${x} -lt 10 ]
    do
       find_process_count
       if [ ${PROC_COUNT} -lt 1 ]; then
         x=100
         find_process
         echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} IS STOPPED..."
         echo " * [$(date +"%F_%H:%M:%S")] : TASK RESULT => SUCCESS"
         echo " * [$(date +"%F_%H:%M:%S")] : PLEASE READ THE LOG FILE"
         print_end
       fi
       x=$((x+1))
       sleep 1
    done
    echo " * [$(date +"%F_%H:%M:%S")] : TASK RESULT => CONFIRM(STOP WAIT TO LONG OR NO STOP)"
    print_end
}


stop () {

    print_start
    find_process_count
    echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} PROCESS COUNT : ${PROC_COUNT}"
    if [ ${PROC_COUNT} -lt 1 ]; then
      echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} IS NOT LOADED. THERE IS NO SENSE IN STOP."
      print_end
    fi

    find_process
    echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} RUNNING PROCESS PID : ${PID}"
    if [ -z ${STOP_PORT} ]; then
        echo "stop" > stop.txt
    else
        curl -XGET 'localhost:${STOP_PORT}/stop'
    fi
    echo "    => Please Wait Task End...Confirm sys.log"

    x=1
    while [ ${x} -lt 10 ]
    do
       find_process_count
       if [ ${PROC_COUNT} -lt 1 ]; then
         x=100
         find_process
         echo " * [$(date +"%F_%H:%M:%S")] : ${MODULE_NAME} IS STOPPED..."
         echo " * [$(date +"%F_%H:%M:%S")] : TASK RESULT => SUCCESS"
         echo " * [$(date +"%F_%H:%M:%S")] : PLEASE READ THE LOG FILE"
         print_end
       fi
       x=$((x+1))
       sleep 1
    done
    echo " * [$(date +"%F_%H:%M:%S")] : TASK RESULT => CONFIRM(STOP WAIT TO LONG OR NO STOP)"
    print_end
}

###########################################
# BUILD INFO METHOD
###########################################
build_info () {
    echo ""
    echo "================================================================"
    echo " * ${MODULE_NAME} JAR BUILD INFO"
    echo "   - JDK : ${JDK}"
    echo "   - JAR : ${JAR}"
    echo "----------------------------------------------------------------"
    unzip -p ${JAR} META-INF/MANIFEST.MF
    echo "================================================================"
    echo ""
}

case $1 in
start)
        start
        ;;
stop)
        stopF
        exit 0
        ;;
stopF)
        stopF
        exit 0
        ;;
restart)
        stop
        sleep 2
        start
        ;;
info)
        build_info
        ;;
*)
        echo $"Usage: $0 {start|stop|stopF|restart}"
        RETVAL=1
esac
exit 0
