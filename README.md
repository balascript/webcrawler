# INTRODUCTION

The main objective of the project is to crawl web pages, store them, remove noise in them using Java and finally verify the Zipf's law distribution with the obtained noise-free content.

# ARCHITECTURE

CRAWLER CONTROLLER

 The CrawlerController is the main component of the project. The CrawlerController possesses all the data (List of pages visited, Links from the crawled pages, Multiset) and it provides the Interface, CrawlerControllerCommunicator, to Crawlers to send and receive these data. The Crawler Controller handles all the Crawler threads and monitor their status, synchronization (Basically to prevent deadlock).  The communication interface exposes 3 methods.

- **boolean isOkToProceed(String url,Crawler c)**

This method tells the crawler whether to proceed with the given URL. When returned false, the crawler thread goes to wait state. This helps the controller to keep limit the number of pages crawled.

- **void addNewVisitedPage(Page page)**

This methods helps the crawler thread to pass the crawled page to its controller so that all the links from the crawled page is obtained and links from each host is sent to its respective crawler or a new crawler is created. This is managed by a simple HashMap. Also each URL given by a page is checked for duplicates and checked with the previously visited pages to prevent duplicate crawls. Once a URL gets cleared all these steps, it goes to its respective crawler (based on the host).

- **ThreadGroup getCrawlersGroup()**

This method returns the main ThreadGroup where all the crawlers belongs to.

 In addition to the Crawlers, the controller possesses an additional thread to monitor the Crawlers, HashMap, List of visited pages, and stop all the crawler threads when needed. This monitor thread, once stops all the crawler threads, proceeds with storing the crawled pages to disk and report generation.

CRAWLER

 The Crawler's job is to crawl a page and return it to the Controller. The Controller creates and maintains the Crawler. Once a Crawler gets an URL of particular host, say abc.com, all the upcoming

'to be crawled' links from that host will be crawled only by that Crawler. This helps in following the politeness policy of the web hosts. The list of URL is maintained in a LinkedList which is filled by its Controller. The crawler uses a Java Thread to perform its task, which is started whenever the crawler is created. Whenever the URL list is empty, it goes to wait state relinquishing its CPU. The thread gets awakened by its controller when it re-fills the URL list. A web page for a given URL is crawled using Jsoup

#
library. Certain URLs are not allowed to be crawled by robots. To follow the robot exclusion policy, each URL, before crawling, is checked with the Jrobotx
#
 to make sure it is permitted for our USER AGENT. To follow politeness policy, a delay of 300ms is added before each crawl.

 There are certain cases where a web page crawl gets failed due to network failure or server error, etc. In that case, the HTTP error code is noted against the URL and it will be shown at the end report and the respective page's content will not be considered for verifying Zipf's law.

MONITOR THREAD

 All the crawler threads are controlled by the monitor thread created by the controller. It checks for the number of threads created for crawling purpose, number of pages crawled totally, number of hosts that have been encountered by the links for every 100ms. Whenever it sees the number of crawled pages reach the limit, it interrupts the crawler threads to stop them using the ThreadGroup.

 DISK STORE

 After the crawling is done, all the crawled pages will be held in physical memory for noise removing and storing into disk. The noise removal part will be discussed later. The raw html, without any additional resource (images, css, js, etc.), will be stored into a specific directory named based on its host.

# NOISE REMOVAL

 This is the very important and toughest part of the project. A small study has been made on the Boilerpipe

#
, a good content extractor library written by Dr.Kohlschutter and theories on extracting content based on Text-To-Tag ratio. The main objective is to remove ads, navigation bars, footers and sidebars because these are the places where the main content is not focused. The noise removal part, first was tried with Boilerpipe
#
alone and the text document was observed. It was not perfect as expected, but still that can serve as a benchmark for a content extractor [See Appendix C].

  Then, for the noise removal part, based on the main idea, few parts of the HTML is marked unwanted because of one or more reasons mentioned below.

- The element is a Header or its class name starts with one of the names head, header, or its id starts with one of the names head, header.
- The element is a Footer or its class name or id starts with footer.
- The element is a nav or its class or id denotes it is a navbar.
- The element is an aside or its class or id denotes it is a sidebar.
- The element contains links to facebook, twitter, youtube, pintrest.

In addition to all these, the element's class and id is checked whether it is an advertisement. Most of the advertisements are image or script based and not text based. But still the DOM element which holds the ad can be holding text (eg. alt) can be a noise. Hence all the types of adverts are removed based on AdblockPlus's easylist

# .
Elements used for social icons, breadcrumbs, iframes, embeds are also removed. Finally, the remaining html is converted to plain text using Jsoup's HtmlToPlainText with its source changed a little bit. The noise free text is evaluated with both Boilerpipe's output and with manual human eyes. Appendix-C contains samples of noise removed content for various web pages. Once the noise is removed, the content is added to a new file with the name of its html page in disk followed by "\_less.txt".
# CHALLENGES FACED

 There are few notable challenges that occurred during the early development (kick start stage) of the crawler, mainly implementing the crawler using multiple threads. Plenty of deadlocks occurred and program used to hang. The idea was to create a new crawler for each host. That ended up creating few thousand threads that spoiled the synchronization [see Appendix for screenshots].  Then the number of threads has been limited to 10-15 and a Hashmap is used to store crawlers against a host.  Then a common interface has been created so that the threads can talk to controller through a single channel, to help in synchronization.

 The noise removal part was the most challenging one. Initially the project was about to just use the Boilerpipe

#
 library, which uses the shallow text features to remove the boilerplate from the web page. The main objective of the library is to extract the core content of any type of web page and ours is to just remove the noise and get closer to the core content. Understanding the Boilerpipe
#
, our content extractor was made. Few results are compared to Boilerpipe and it was similar [see Appendix-C for comparison].

 The Robot exclusion policy was hart to implement. Jrobotx saved us a plenty of time.

Jrobotx reads a host's robots.txt file for a given URL and checks whether the given URL for the given user-agent is permitted. Once done, it stores the policy in memory for faster processing of upcoming URLs for same host.

# REPORT HTML

The crawled web pages along with the domain and the corresponding HTTP codes are stored in the

Html file. The html page contains two pie charts that depict the domains and http codes, and a Zipf's word frequency-rank graph. The html files also displays the total number of pages, number of domains the crawler encountered. The html page is generated using Mustache java library using templates. The charts are drawn using Zingchart and Google Charts, styled using Bootstrap. Appendix D, E, F contains few sample reports.

# ZIPF'S LAW VERIFICATION

  Based on this experiment to verify Zipf's law for the obtained bunch of text, it has been observed that the Zipf's law hold good when the size of the content increases and it is not concentrated at the starting and ending of the chart drawn using word frequencies.

It has been observed that, the Zipf's distribution highly depends on our noise removal method. Because before removing noise, the words

"http", "home", "copy" showed up in the top 10 word frequencies and after making this noise removal method better, the graph seems to be pretty close to Rt\*ft=k (at least at certain part of the graph).



# APPENDIX - B

_PROJECT FACTS_

  There are certain rules that are applicable for this project. These rules will help you understand the execution and output of this project.

The  input  of  the  program  is  a  csv  file  in  the  following  format

<seedurl with protocol>,<limit\_in\_number>,<domain\_restriction\_as\_boolean>

The third parameter domain\_restriction\_as\_boolean is optional. By default it is false.

Hosts "www.scu.edu" and "scu.edu" are different.

All valid URLs (by Apache commons validator)

#
inside href of an anchor tag of a crawled page is considered as a link.

A working internet connection is needed to view report.html. It uses external CDNs to get Bootstrap, Jquery, Loader and ZingChart.

Increasing the number of crawlers helps to reach the limit faster but not speeds the content processing because, it runs on a single thread.

Having too many crawlers can affect synchronization. Based on experiment, do not exceed 10.

The system used to develop and test this crawler had a physical memory of 8GB and CPU speed of 2.4GHz max. We reached 80% of memory and 60% of CPU during tests. It took 30 minutes to crawl, process and generate report for 1000 pages. Plan your time ahead to test this crawler.

The crawler doesn't check disk space before storing HTML pages and noise removed texts, beware.

When the controller runs out of new links to give to crawler threads, they wait till eternity for new links. Monitor thread runs forever counting the active crawlers and number of pages crawled. Limit won't be reached.

When an exception occurs while downloading html from internet, the URL is ignored and not marked as visited.

The crawler and Zipf's distribution has been tested only with English websites.

It has been observed URL duplicates in the report (very very rare) despite having Java Hashsets and enforced synchronization.

After using Adblockplus.org's EasyList, no ad content has been found in the noise removed text.

# REFERENCES

[http://jsoup.org](http://jsoup.org/)

[https://github.com/TrigonicSolutions/jrobotx](https://github.com/TrigonicSolutions/jrobotx)

[https://github.com/kohlschutter/boilerpipe](https://github.com/kohlschutter/boilerpipe)

[https://github.com/spullara/mustache.java](https://github.com/spullara/mustache.java)

[http://opencsv.sourceforge.net/](http://opencsv.sourceforge.net/)

[https://adblockplus.org/en/filters#basic](https://adblockplus.org/en/filters#basic)

[https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/HtmlToPla](https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/HtmlToPlainText.java) [inText.java](https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/HtmlToPlainText.java)

[https://github.com/google/guava](https://github.com/google/guava)

[https://lucene.apache.org/core/4\_0\_0/](https://lucene.apache.org/core/4_0_0/)

[https://commons.apache.org/proper/commons-validator/](https://commons.apache.org/proper/commons-validator/)

[http://jsoup.org/cookbook/input/load-document-from-url](http://jsoup.org/cookbook/input/load-document-from-url)

[http://stackoverflow.com/questions/1600291/validating-url-in-java](http://stackoverflow.com/questions/1600291/validating-url-in-java)

[http://howtodoinjava.com/core-java/multi-threading/how-to-work-with-wait-notify-and](http://howtodoinjava.com/core-java/multi-threading/how-to-work-with-wait-notify-and-notifyall-in-java/) [notifyall-in-java/](http://howtodoinjava.com/core-java/multi-threading/how-to-work-with-wait-notify-and-notifyall-in-java/)

[http://stackoverflow.com/questions/3771081/proper-way-to-check-for-url-equality](http://stackoverflow.com/questions/3771081/proper-way-to-check-for-url-equality)

[http://stackoverflow.com/questions/35128092/threadgroup-in-java](http://stackoverflow.com/questions/35128092/threadgroup-in-java)

[http://www.tutorialspoint.com/java/java\_thread\_communication.htm](http://www.tutorialspoint.com/java/java_thread_communication.htm)

[http://www.roseindia.net/java/beginners/java-create-directory.shtml](http://www.roseindia.net/java/beginners/java-create-directory.shtml)

[http://stackoverflow.com/questions/27399419/giving-current-timestamp-as-folder](http://stackoverflow.com/questions/27399419/giving-current-timestamp-as-folder-name-in-java) [name-in-java](http://stackoverflow.com/questions/27399419/giving-current-timestamp-as-folder-name-in-java)

[http://stackoverflow.com/questions/1922677/nullpointerexception-when-creating-an](http://stackoverflow.com/questions/1922677/nullpointerexception-when-creating-an-array-of-objects) [array-of-objects](http://stackoverflow.com/questions/1922677/nullpointerexception-when-creating-an-array-of-objects)

[http://videolectures.net/wsdm2010\_kohlschutter\_bdu/](http://videolectures.net/wsdm2010_kohlschutter_bdu/)

[http://www.philippeadjiman.com/blog/2009/10/26/drawing-the-long-tail-of-a-zipf-law](http://www.philippeadjiman.com/blog/2009/10/26/drawing-the-long-tail-of-a-zipf-law-using-gnuplot-java-and-moby-dick/) [using-gnuplot-java-and-moby-dick/](http://www.philippeadjiman.com/blog/2009/10/26/drawing-the-long-tail-of-a-zipf-law-using-gnuplot-java-and-moby-dick/)

[http://www.businessinsider.com/henry-blodget-bing-revisited-still-toast-but-slightly](http://www.businessinsider.com/henry-blodget-bing-revisited-still-toast-but-slightly-less-burnt-2010-3) [less-burnt-2010-3](http://www.businessinsider.com/henry-blodget-bing-revisited-still-toast-but-slightly-less-burnt-2010-3)

[http://stackoverflow.com/questions/5640334/how-do-i-preserve-line-breaks-when](http://stackoverflow.com/questions/5640334/how-do-i-preserve-line-breaks-when-using-jsoup-to-convert-html-to-plain-text) [using-jsoup-to-convert-html-to-plain-text](http://stackoverflow.com/questions/5640334/how-do-i-preserve-line-breaks-when-using-jsoup-to-convert-html-to-plain-text)

[http://hellospark.com/en-us/blog/invest-google-adwords-bing/](http://hellospark.com/en-us/blog/invest-google-adwords-bing/)
