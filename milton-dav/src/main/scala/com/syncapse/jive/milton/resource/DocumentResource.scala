package com.syncapse.jive.milton.resource

import com.bradmcevoy.http._
import java.util.Map
import java.lang.String
import com.jivesoftware.community.{DocumentManager, Document}
import org.apache.commons.io.IOUtils
import java.io.{InputStream, OutputStream}
import com.syncapse.jive.Loggable

class DocumentResource(val document: Document,
                       private val dm: DocumentManager,
                       private val sm: SecurityManager)
        extends BaseResource(sm)
        with DeletableResource
        with MoveableResource
        with PropFindableResource
        with Loggable {

  def getModifiedDate = document.getModificationDate

  def getName = document.getBinaryBody.getName

  def getUniqueId = document.getDocumentID

  def getCreateDate = document.getCreationDate

  def delete = dm.deleteDocument(document)

  override def getContentLength = document.getBinaryBody.getSize

  def getContentType(accepts: String) = document.getBinaryBody.getContentType

  def sendContent(out: OutputStream, range: Range, params: Map[String, String], contentType: String) = {
    val in: InputStream = document.getBinaryBody.getData
    try {

      IOUtils.copy(in, out)
      out.flush

    } finally {
      IOUtils.closeQuietly(in)
    }

  }

  def moveTo(rDest: CollectionResource, name: String) = rDest match {
    case cr: CommunityResource => dm.moveDocument(document, cr.community)
    case _ => logger.warn("Do not know how to move resource to " + name)
  }

}