APP_NAME="twm"
APP_VERSION="2.0-SNAPSHOT"
JAVA_PARAM="-Xmx2g"

BIN_PATH=$TWM_HOME_PARENT/TWM/$APP_NAME/bin     #TWM-HOME-PARENT :: exported in .bashrc
JAR_PATH=$BIN_PATH/../target/$APP_NAME-$APP_VERSION.jar
JAVA_PATH=$HOME/.jdks/corretto-17.0.9/bin/java

echo "Starting '$APP_NAME' with java param: '$JAVA_PARAM', at '$JAR_PATH'"
$JAVA_PATH $JAVA_PARAM -jar $JAR_PATH
