//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.01.14 at 09:56:57 AM CET 
//


package org.iaea._2012.irix.format.releaseinformation;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ReleaseToAirPhasesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ReleaseToAirPhasesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ReleasePhase" type="{http://www.iaea.org/2012/IRIX/Format/ReleaseInformation}ReleaseToAirPhaseType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReleaseToAirPhasesType", propOrder = {
    "releasePhase"
})
public class ReleaseToAirPhasesType {

    @XmlElement(name = "ReleasePhase", required = true)
    protected List<ReleaseToAirPhaseType> releasePhase;

    /**
     * Gets the value of the releasePhase property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the releasePhase property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReleasePhase().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ReleaseToAirPhaseType }
     * 
     * 
     */
    public List<ReleaseToAirPhaseType> getReleasePhase() {
        if (releasePhase == null) {
            releasePhase = new ArrayList<ReleaseToAirPhaseType>();
        }
        return this.releasePhase;
    }

}
