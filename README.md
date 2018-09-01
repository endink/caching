# Labijie Caching
A cache structure that supports expiration on a per-key basis.


![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/labijie/labijie-caching/maven-metadata.xml.svg)](http://central.maven.org/maven2/com/labijie/labijie-caching)

All of the jar packages has been uploaded to the maven central.


## add depenedency in gradle project

Use memory cahce only:
```groovy
dependencies {
    compile "com.labijie:labijie-caching:1.0"
}
```

for redis:
```groovy
dependencies {
    compile "com.labijie:labijie-caching-redis:1.0"
}
```

# Quick Start
You only need to use the ICacheManager interface, which can be easily integrated into the IOC container, such as spring.
use 

```
ICacheManager memoryCache = new MemoryCache(new MemoryCacheOptions());

//sliding time expires
memoryCache.set("2", new Object(), 3000L, true);

//absolute time expires
memoryCache.set("a", new Object(), 1000L, false);

//get
memoryCache.get("a")

```


use SingleRedisCacheManager for redis support

# Maven local usage

## install to maven local repo:
gradle uploadArchives

## install to public maven

modify build.gradle and put your maven server (such as nexus server):

for example , you server is 'https://nexus-server/', user name is [my], password is [mypwd]
1. modify build.gradle:
```groovy
mavenDeployer {
                        pom.project {
                            artifactId "labijie-caching" + (project.name == "core" ?  "" : "-" + project.name)
                            version project.rootProject.version
                        }

                        snapshotRepository(url: "https://nexus-server/") {
                            authentication(userName: u, password: p)
                        }

                        repository(url: "https://nexus-server/") {
                            authentication(userName: u, password: p)
                        }
                    }
```  

2. run command in terminal

gradle -Dmu="my" -Dmp="mypwd" uploadArchives

