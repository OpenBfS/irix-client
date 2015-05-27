//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.01.14 at 09:56:57 AM CET 
//


package org.iaea._2012.irix.format.meteorology;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.iaea._2012.irix.format.base.InformationBlock;


/**
 * <p>Java class for MeteorologyType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MeteorologyType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.iaea.org/2012/IRIX/Format/Base}InformationBlock">
 *       &lt;sequence>
 *         &lt;element name="MeteoRecord" type="{http://www.iaea.org/2012/IRIX/Format/Meteorology}MeteoRecordType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MeteorologyType", propOrder = {
    "meteoRecord"
})
public class MeteorologyType
    extends InformationBlock
{

    @XmlElement(name = "MeteoRecord", required = true)
    protected List<MeteoRecordType> meteoRecord;

    /**
     * Gets the value of the meteoRecord property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the meteoRecord property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMeteoRecord().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MeteoRecordType }
     * 
     * 
     */
    public List<MeteoRecordType> getMeteoRecord() {
        if (meteoRecord == null) {
            meteoRecord = new ArrayList<MeteoRecordType>();
        }
        return this.meteoRecord;
    }

}
