package com.syncapse.jive.webdav.fromcatalina

import java.io.Writer
import java.util.Map
  
object XMLWriter {

  /**
      * Opening tag.
      */
     val  OPENING = 0

     /**
      * Closing tag.
      */
     val  CLOSING = 1

     /**
      * Element with no content.
      */
     val NO_CONTENT = 2

}

/**
 * XMLWriter helper class.
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 */
class XMLWriter(namespaces: Map[String, String]) {



    // ----------------------------------------------------- Instance Variables

    /**
     * Buffer.
     */
    protected val _buffer = new StringBuffer()

    /**
     * Is true until the root element is written
     */
    protected val _isRootElement = true

    /**
     * Retrieve generated XML.
     *
     * @return String containing the generated XML
     */
    def toString() = _buffer.toString()

    /**
     * Write property to the XML.
     *
     * @param name
     *      Property name
     * @param value
     *      Property value
     */
    def writeProperty(name: String, value: String) = {
        writeElement(name, OPENING)
        _buffer.append(value)
        writeElement(name, CLOSING)
    }

    /**
     * Write property to the XML.
     *
     * @param name
     *      Property name
     */
    def writeProperty(name: String ) = {
        writeElement(name, XMLWriter.NO_CONTENT)
    }

    /**
     * Write an element.
     *
     * @param name
     *      Element name
     * @param type
     *      Element type
     */
    def writeElement(name:String, xmlType: Int) = {
        val nsdecl = new StringBuffer()

        if (_isRootElement) {
            for (fullName <- namespaces) {
                val abbrev = namespaces.get(fullName)
                nsdecl.append(" xmlns:").append(abbrev).append("=\"").append(
                        fullName).append("\"")
            }
            _isRootElement = false
        }

        val pos = name.lastIndexOf(':')
        if (pos >= 0) {
            // lookup prefix for namespace
            val fullns = name.substring(0, pos)
            val prefix = namespaces.get(fullns)
            if (prefix == null) {
                // there is no prefix for this namespace
                name = name.substring(pos + 1)
                nsdecl.append(" xmlns=\"").append(fullns).append("\"")
            } else {
                // there is a prefix
                name = prefix + ":" + name.substring(pos + 1)
            }
        } else {
            throw new IllegalArgumentException(
                    "All XML elements must have a namespace")
        }

        xmlType match {
          case OPENING =>
              _buffer.append("<" + name + nsdecl + ">")
          case CLOSING =>
              _buffer.append("</" + name + ">\n")
          case NO_CONTENT =>
              // do nothing
          case _=>
              _buffer.append("<" + name + nsdecl + "/>")
        }
    }

    /**
     * Write text.
     *
     * @param text
     *      Text to append
     */
    def writeText(text:String ) = {
        _buffer.append(text)
    }

    /**
     * Write data.
     *
     * @param data
     *      Data to append
     */
    def writeData(data: String) = {
        _buffer.append("<![CDATA[" + data + "]]>")
    }

    /**
     * Write XML Header.
     */
    def writeXMLHeader = {
        _buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n")
    }

}