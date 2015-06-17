![Build Status](https://travis-ci.org/kog/hss.svg?branch=master)

# hss - HTTP Streaming Service

## Fork Implementation

The original text is preserved below the line break, and includes the requirements: both required and optional. The implementation attempts to cover both the minimum required functionality as well as the stretch goals. Hopefully there is a good balance between what you would do in the real world as opposed to shortcuts taken for the purposes of be a pedagogical example.

### How do I run this?
 * Get Java8 from Oracle (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
 * Grab Maven 3 from Apache (http://maven.apache.org/download.cgi)
 * Put them both in your path and set up the relevant environment variables.

Once this is done, you should be able to navigate to the root of the Git repo and:

```
mvn clean install && mvn jetty:run
```

This will go through a full build cycle, building the source and running both unit and integration tests. After the build cycle, Maven will kick off a Jetty 9 container running the compiled WAR file. You can then navigate to **http://localhost:8080/hss** and you will be greeted with a [Swagger](http://swagger.io) based sandbox.

Please note that the sandbox has a few caveats:
 * Using binary data may not be possible (or easy).
 * Order of plugins (GET/PUT/POST verbs) matters, and may not be easily configured.
 * Data will not persist between runs (see below).


### What is this?

This is a Java 8, Jetty 9 compatible web appliction, written (unsurprisingly) in Java. The build tool of choice is Maven. There is an embedded HSQLDB that tracks the state, but does not attempt to persist any data outside of the immediate WAR context. So, if you run the demo and shut it down, the persistence will be destroyed. The files will hang around. Why? Because again, this is a pedagogical example heh.

This would be considered a Richardson Maturity level 2. It doesn't necessarily make a lot of sense to use Hypermedia here.

### Why did you (whatever)?

You'll notice that there are still some TODO floating around the source, acknowledging that while the app satisfies all outlined requirements (core, stretch) there is still room for improvement. The guidance given was that this app should not take "too long" to write - which makes sense since it's just a demo. On the other hand, it's not a very convincing demo if everything is just bailing wire and duct tape: to convince people you're clever/good at your job, you need to show some skills...

I'd also point out that S3 was so non-trivial that Amazon hired an entire team of people to write it, heh.

Technology choices are based around writing actual, scalable software in the real world. You certainly could use any number of different technologies from PHP to Node.JS to ASP.NET. These are technologies I've used to do the job, and technologies that - unless someone can point to something better - I'd be likely to use again. There are a set of heuristics involved in the decision that may not be readily obvious. Frankly, I also didn't have any new, shiny technology I wanted to try instead ;) If you've got suggestions...

OK, so I lied a little: I hadn't used Swagger in years, I've never used Java 8, Jetty 9, or Jersey 2.x. It turns out that I found a few new headaches I never knew I was in for. Guess it was a learning experience after all.

As for documentation it really came down to [API Blueprint](https://apiblueprint.org/) or [Swagger](http://swagger.io), and it looked like the latter was a better choice for what I was doing. Api Blueprint can really be quite verbose.

### Application/Octet-Stream

The initial requirement notes a "json preamble," which could be solved in one of a few different ways:
 * Multipart-mime: one part is the JSON preamble, one part is the file.
 * Attempting to prepend JSON to the front of the stream, using application/octet-stream.
 * Base64 encoding a blob or something.
 * I'm sure there's some other way I'm forgetting.

Multipart has a number of issues, one of which being that many implementations require the entire document to be in memory, as many parsers need the entire document to find the boundaries. Further, using multipart can cause a number of problems with client libraries, and may not be very intuitive. It's also completely unecessary.

If we were to use JSON concatenated onto a byte stream, it would not only be painful to marshal, but no one would ever be able to use it. No one really ever does this, sort short of some BASH wizardry (or PoSH, or whatever) you'd probably be unable to use the APIs.

The base64 solution is reductive of the multipart-mime: it's completely pointless. You can accomplish the same functionality using query parameters - especially since all we care about are the filters/actions we're running over the streams.  

As we're not uploading files to temporary locations, all streams are filtered inline, using stream transformation. A downside is that if an upload is failed, we mark the ID as failed and it becomes unavailable to get. That being said, the filters are all pretty artificial to begin with: GZIP would be handed via an encoding header, and there's pretty much no condition under which the encryption would not be opaque to the caller as it does not add any security. Still, it was in the requirements ;)

### A note on encryption

The original problem statement specified that one of the plugins for stream processing should be encryption. I asked about this, and I gather that this project is mostly about being vague and seeing what I'll do in response. So, I wrote a token bit of encryption that would use a shared secret, configured in the Spring wiring, to do symmetric encryption (in this case, AES). It's in a block cipher mode (CBC).

If you wanted to meaningfully encrypt the blobs, you probably wouldn't use symmetric encryption - or at least not directly. It certainly wouldn't make sense to pass in the secrets via headers/query parameters/HTTP specific mechanisms: if they were intercepted, they'd be worthless. They'd also be vulnerable to playback attacks. Signing the payload is wrong since it needs to be secret. You could use asymmetric crypto (IE: encrypt it to my public key), but the server would need another channel so that the key is exchanged out-of-band, and known a priori. Which I don't.

I think the better question is "what type of problem are we trying to solve?"" In the absence of a real answer my best guess is that the encryption would actually be opaque to the user, and that it would be done by the provider (IE: someone like Amazon) to ensure the confidentiality of your files. Maybe your call would include a token that links to an account that has a shared secret/key on file.

In the meantime, the implementation that I chose to do is one step above ROT26+Base64, heh.

That all being said, I have this horrible dread that in my rush to be cheeky I missed something completely obvious.

### Rough Edges

Again, it's a balance between production grade code vs. having a life. But here are a few things that you'd probably care about:

 * Improving the persistence technology: something like JPA/Hibernate, or at the very least using something like C3P0 with a real RDBMS. Maybe some sort of NoSQL.
 * You'd probably want to use transport layer security.
 * Auth/Auth, probably some sort of account-based bucketing too.
 * Possible ETag support.
 * Monitoring.
 * Versioning.
 * Bucketing on your disk - right now everything is in a single directory, and this is really bad. You'd probably also want to do some sort of sharding anyway, as well as replication.
 * Pagination of the files (along with sorting criteria). Right now you just get back whatever is in the system in one splat.
 * I18N/L10N.
 * Better clustering support - maybe some sort of shared state for things like locking.
 * Byte range resumption on the GETs (or even posts?).
 * Logging (put in the bare minimum here, and I would probably look at Lo4J2).
 * Better "encryption" - see above.
 * Robust error handling - at least an ExceptionMapper, if nothing else...
 * Better handling of the FAILED state.
 * Improved filter support.
 * Javadocs, CI loop (though, I've got Travis running for now).

You'd still want a persistence-based solution, so that the cluster can share the state of what's going on (acquire locks, etc). You might move to a versioning based solution, or use something like UUIDs linked to tags (or even requesting/reserving an identifier). There'd probably also need to be better support for versioning/snapshotting the blobs (IE: maintain at least 3-4 previous revisions).

---

## Original text


### Problem statement
Create a RESTful web service that given a data stream via PUT or POST, with a json preamble, create a stream object with a pluggable architecture for processing the stream. (i.e. zip or encrypt the stream or both)

### Requirements
* Whatever language you are comfortable using
* Maintain your code in a fork of this repo
* RESTful Interface transport over HTTP
* GET input for retrieving the resulting processed stream
* JSON preamble describing the stream

### Bonus
* Generic GET endpoint to list previous results
* Display state (success,failure,processing) while listing previous results
* Reverse the behavior during a specific GET
* Select pluggins via the JSON preamble
* DEL endpoint deletes temporary files and processed streams

### Presentation
Present your work to our team via screen sharing session. Talk about the approach you took. Demo.
