
package org.alex73.skarynka.scan.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}settings"/>
 *         &lt;element ref="{}permissions" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}page-tags"/>
 *         &lt;element ref="{}process-commands"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "settings",
    "permissions",
    "pageTags",
    "processCommands"
})
@XmlRootElement(name = "config")
public class Config {

    @XmlElement(required = true)
    protected Settings settings;
    protected List<Permissions> permissions;
    @XmlElement(name = "page-tags", required = true)
    protected PageTags pageTags;
    @XmlElement(name = "process-commands", required = true)
    protected ProcessCommands processCommands;

    /**
     * Gets the value of the settings property.
     * 
     * @return
     *     possible object is
     *     {@link Settings }
     *     
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Sets the value of the settings property.
     * 
     * @param value
     *     allowed object is
     *     {@link Settings }
     *     
     */
    public void setSettings(Settings value) {
        this.settings = value;
    }

    /**
     * Gets the value of the permissions property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the permissions property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPermissions().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Permissions }
     * 
     * 
     */
    public List<Permissions> getPermissions() {
        if (permissions == null) {
            permissions = new ArrayList<Permissions>();
        }
        return this.permissions;
    }

    /**
     * Gets the value of the pageTags property.
     * 
     * @return
     *     possible object is
     *     {@link PageTags }
     *     
     */
    public PageTags getPageTags() {
        return pageTags;
    }

    /**
     * Sets the value of the pageTags property.
     * 
     * @param value
     *     allowed object is
     *     {@link PageTags }
     *     
     */
    public void setPageTags(PageTags value) {
        this.pageTags = value;
    }

    /**
     * Gets the value of the processCommands property.
     * 
     * @return
     *     possible object is
     *     {@link ProcessCommands }
     *     
     */
    public ProcessCommands getProcessCommands() {
        return processCommands;
    }

    /**
     * Sets the value of the processCommands property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProcessCommands }
     *     
     */
    public void setProcessCommands(ProcessCommands value) {
        this.processCommands = value;
    }

}
