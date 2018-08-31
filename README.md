# caching
A cache structure that supports expiration on a per-key basis.

example:
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

# Maven usage

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

## add depenedency in gradle project

dependencies {
    compile "com.labijie:labijie-caching:1.0"
    compile "com.labijie:labijie-caching-redis:1.0"
}