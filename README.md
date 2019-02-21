# npa-svc

## Requirements

- [Apache Maven 3.x](http://maven.apache.org)

## Preparing

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
