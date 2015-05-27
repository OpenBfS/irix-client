//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.01.16 at 10:55:05 AM CET 
//


package org.iaea._2012.irix.format.base;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 * 				A percentage ranging from 0 till 100 (both included). This is the
 * 				type used in IRIX
 * 				instance documentents. This type has a fixed,
 * 				required attribute with value
 * 				'%'.
 * 				
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;html:br xmlns:html="http://www.w3.org/1999/xhtml" xmlns:base="http://www.iaea.org/2012/IRIX/Format/Base" xmlns:xsd="http://www.w3.org/2001/XMLSchema"/&gt;
 * </pre>
 * 
 * 				Example:
 * 				
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;Humidity xmlns:base="http://www.iaea.org/2012/IRIX/Format/Base" xmlns:html="http://www.w3.org/1999/xhtml" xmlns:xsd="http://www.w3.org/2001/XMLSchema" Unit="%"&gt;77.3&lt;/Humidity&gt;
 * </pre>
 * 
 * 				.
 * 			
 * 
 * <p>Java class for PercentageType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PercentageType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.iaea.org/2012/IRIX/Format/Base>PercentageValueType">
 *       &lt;attribute name="Unit" use="required" type="{http://www.w3.org/2001/XMLSchema}token" fixed="%" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PercentageType", propOrder = {
    "value"
})
public class PercentageType {

    @XmlValue
    protected BigDecimal value;
    @XmlAttribute(name = "Unit", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String unit;

    /**
     * 
     * 				A percentage value
     * 				ranging from 0 till 100 (both included). This type
     * 				is
     * 				used to build
     * 				the base:PercentageType. This type should not be used in
     * 				a XML
     * 				instance document.
     * 			
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setValue(BigDecimal value) {
        this.value = value;
    }

    /**
     * Gets the value of the unit property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUnit() {
        if (unit == null) {
            return "%";
        } else {
            return unit;
        }
    }

    /**
     * Sets the value of the unit property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUnit(String value) {
        this.unit = value;
    }

}
