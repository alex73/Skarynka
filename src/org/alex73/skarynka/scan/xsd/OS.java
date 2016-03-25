
package org.alex73.skarynka.scan.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for OS.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="OS">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Linux"/>
 *     &lt;enumeration value="Windows"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "OS")
@XmlEnum
public enum OS {

    @XmlEnumValue("Linux")
    LINUX("Linux"),
    @XmlEnumValue("Windows")
    WINDOWS("Windows");
    private final String value;

    OS(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static OS fromValue(String v) {
        for (OS c: OS.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
