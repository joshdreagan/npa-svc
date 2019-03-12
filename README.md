# npa-svc

## Requirements

- [Apache Maven 3.x](http://maven.apache.org)
- [MySQL 5.7.18](https://www.mysql.com/oem/)
  - [Docker Image](https://hub.docker.com/r/mysql/mysql-server/)

## Preparing

Install and run MySQL [https://dev.mysql.com/doc/refman/5.7/en/installing.html]

_Note: For my tests, I chose to run the docker image [https://hub.docker.com/r/mysql/mysql-server/]. You can run it using the command `docker run --name mysql -e MYSQL_DATABASE=example -e MYSQL_ROOT_PASSWORD=Abcd1234 -e MYSQL_ROOT_HOST=172.17.0.1 -p 3306:3306 -d mysql/mysql-server:5.7`. You can then connect and run SQL statements using the command `docker exec -it mysql mysql -uroot -p`._

Build the project source code

```
cd $PROJECT_ROOT
mvn clean install
```

## Running the example standalone

```
cd $PROJECT_ROOT
mvn spring-boot:run
```

## Running the example in OpenShift

```
oc new-project demo
oc create -f src/main/kube/serviceaccount.yml
oc create -f src/main/kube/configmap.yml
oc create -f src/main/kube/secret.yml
oc secrets add sa/npa-svc-sa secret/npa-svc-secret
oc policy add-role-to-user view system:serviceaccount:demo:npa-svc-sa
mvn -P openshift clean install fabric8:deploy
```

## Testing the code

To upload bulk CSV data you can use `curl` (as seen below), or you can use the upload form at 'http://localhost:8080/upload'.

```
curl -X POST -F '@file=@./src/test/data/NANPA_BY_STATE.csv' 'http://localhost:8080/camel/npa/upload/'
```

To get all NPA codes:

```
curl -X GET -H 'Accept: application/json' 'http://localhost:8080/camel/npa'
```

To get a state by NPA code:

```
curl -X GET -H 'Accept: application/json' 'http://localhost:8080/camel/npa/by-code/940'
```

To get a list of NPA codes by state:

```
curl -X GET -H 'Accept: application/json' 'http://localhost:8080/camel/npa/by-state/TX'
```
