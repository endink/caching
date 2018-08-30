# caching
A cache structure that supports expiration on a per-key basis.

example:
```
ICacheManager memoryCache = new MemoryCache(new MemoryCacheOptions());

//sliding expiring
memoryCache.set("2", new Object(), 3000L, true);

//abslution expiring
memoryCache.set("a", new Object(), 1000L, false);

//get
memoryCache.get("a")

```


redis support is coming soon.
