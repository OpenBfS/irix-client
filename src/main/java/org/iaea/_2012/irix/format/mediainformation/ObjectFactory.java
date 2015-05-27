//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.01.14 at 09:56:57 AM CET 
//


package org.iaea._2012.irix.format.mediainformation;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.iaea._2012.irix.format.mediainformation package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _MediaInformation_QNAME = new QName("http://www.iaea.org/2012/IRIX/Format/MediaInformation", "MediaInformation");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.iaea._2012.irix.format.mediainformation
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link MediaInformationType }
     * 
     */
    public MediaInformationType createMediaInformationType() {
        return new MediaInformationType();
    }

    /**
     * Create an instance of {@link PublicInformationContactsType }
     * 
     */
    public PublicInformationContactsType createPublicInformationContactsType() {
        return new PublicInformationContactsType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MediaInformationType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.iaea.org/2012/IRIX/Format/MediaInformation", name = "MediaInformation")
    public JAXBElement<MediaInformationType> createMediaInformation(MediaInformationType value) {
        return new JAXBElement<MediaInformationType>(_MediaInformation_QNAME, MediaInformationType.class, null, value);
    }

}
