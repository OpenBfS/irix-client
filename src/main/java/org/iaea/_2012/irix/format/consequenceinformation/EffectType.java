//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.01.14 at 09:56:57 AM CET 
//


package org.iaea._2012.irix.format.consequenceinformation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.iaea._2012.irix.format.base.FreeTextType;


/**
 * <p>Java class for EffectType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EffectType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="TypeOfEffect" type="{http://www.iaea.org/2012/IRIX/Format/ConsequenceInformation}EffectTypeCodeType" minOccurs="0"/>
 *         &lt;element name="Description" type="{http://www.iaea.org/2012/IRIX/Format/Base}FreeTextType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EffectType", propOrder = {
    "typeOfEffect",
    "description"
})
public class EffectType {

    @XmlElement(name = "TypeOfEffect")
    protected EffectTypeCodeType typeOfEffect;
    @XmlElement(name = "Description")
    protected FreeTextType description;

    /**
     * Gets the value of the typeOfEffect property.
     * 
     * @return
     *     possible object is
     *     {@link EffectTypeCodeType }
     *     
     */
    public EffectTypeCodeType getTypeOfEffect() {
        return typeOfEffect;
    }

    /**
     * Sets the value of the typeOfEffect property.
     * 
     * @param value
     *     allowed object is
     *     {@link EffectTypeCodeType }
     *     
     */
    public void setTypeOfEffect(EffectTypeCodeType value) {
        this.typeOfEffect = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link FreeTextType }
     *     
     */
    public FreeTextType getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link FreeTextType }
     *     
     */
    public void setDescription(FreeTextType value) {
        this.description = value;
    }

}
