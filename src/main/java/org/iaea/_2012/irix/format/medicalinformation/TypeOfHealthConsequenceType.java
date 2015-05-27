//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.01.14 at 09:56:57 AM CET 
//


package org.iaea._2012.irix.format.medicalinformation;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TypeOfHealthConsequenceType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="TypeOfHealthConsequenceType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *     &lt;enumeration value="Amputation"/>
 *     &lt;enumeration value="Contracture"/>
 *     &lt;enumeration value="Fatality"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "TypeOfHealthConsequenceType")
@XmlEnum
public enum TypeOfHealthConsequenceType {

    @XmlEnumValue("Amputation")
    AMPUTATION("Amputation"),
    @XmlEnumValue("Contracture")
    CONTRACTURE("Contracture"),
    @XmlEnumValue("Fatality")
    FATALITY("Fatality");
    private final String value;

    TypeOfHealthConsequenceType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TypeOfHealthConsequenceType fromValue(String v) {
        for (TypeOfHealthConsequenceType c: TypeOfHealthConsequenceType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
