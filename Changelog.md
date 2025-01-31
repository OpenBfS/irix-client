# Changelog

## Version 3.x

### 3.1 *2025-01-31*

 - jaxws, javax.xml.bind, jaxb to current versions (libs and plugins)
 - remove dependency on log4j (use System.Logger instead)
 - remove dependency on Apache HttpClient (use java.net.http.HttpClient instead)
 - fix some tests and temporarily disable others
 - allow ; as separator in rolesHeaderString to support multiple roles from Shibboleth
 - sensible errors when mapfish-print is not responding (was a misleading JSON error)
 - tested running with Java 11 and Java 21 (build version still 11)

### 3.0.0 *2023-10-24* (changes vs. 2.7.1)

 - start of upgrade to Java 11, Java build version: 11
 - removed rpm-plugin for maven
 - upgrade of log4j

## Version 2.x

### 2.7.2 *2024-12-11*

 - allow ; as separator in rolesHeaderString to support multiple roles from Shibboleth

### 2.7.1 *2021-06-25*

 - robust Scenario parsing

### 2.7.0 *2021-06-11*

 - added doGet() to respond 200 OK
 - upgraded Apache HttpClient to 4.5.13

### 2.6.0 *2020-08-14*

 - IRIXDokpool schema updated
 - simpler array support


### 2.5.0 *2019-12-09*

 - IRIX schema 2.5.0
 - docs about web.xml in README

### 2.4.1 *2019-12-05*

 - more robust header configuration

### 2.4.0 *2019-12-04*

 - IRIX schema 2.3.0
 - more robust userJsonObject
 - autofill Dokpool DocumentOwner

### 2.3.0 *2019-11-20*

 - add support for baseurl and printapp in mapfish-print

### 2.2.0 *2019-11-07*

 - add support for linkName
 - create irixconfdir if not existing
 - maven rpm pugin

### 2.1.0 *2018-12-19*

 - Doksys schema change

### 2.0.0 *2018-12-17*

 - support Dokpool DocumentOwner
 - allow reports without attachment
 - many more examples
 - first working status for Rodos and Rei support
 - Java 8 and IRIX Broker / Dokpool Client 2.x

## Version 1.x

### 1.4.1 *2017-10-26*

 - changes reagarding annotations (e.g., umlaut support)

### 1.4.0 *2017-08-30*

 - irix-schema as submodule
 - too many changes to list

### 1.2.0 *2016-02-22*

 - first tagged version in this repo
