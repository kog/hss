# hss
HTTP Streaming Service

# Problem statement
Create a RESTful web service that given a data stream via PUT or POST, with a json preamble, create a stream object with a pluggable architecture for processing the stream. (i.e. zip or encrypt the stream or both)

# Requirements
* Whatever language you are comfortable using
* Maintain your code in a fork of this repo
* RESTful Interface transport over HTTP
* GET input for retrieving the resulting processed stream
* JSON preamble describing the stream

# Bonus
* Generic GET endpoint to list previous results
* Display state (success,failure,processing) while listing previous results
* Reverse the behavior during a specific GET
* Select pluggins via the JSON preamble
* DEL endpoint deletes temporary files and processed streams

# Presentation
Present your work to our team via screen sharing session. Talk about the approach you took. Demo.
