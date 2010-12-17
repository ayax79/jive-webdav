package com.syncapse.jive.milton.resource

import java.lang.{Long, String}
import scala.collection.JavaConversions._
import com.jivesoftware.community._
import com.syncapse.jive.auth.JiveAuthenticationProvidable
import com.syncapse.jive.milton.WebdavUtil
import java.util.{Map => JMap}
import com.bradmcevoy.http._
import java.io.{OutputStream, InputStream}

class CommunityResource(community: Community, jc: JiveContext, sm: SecurityManager)
        extends BaseResource(sm) with PutableResource with JiveAuthenticationProvidable {

  def getModifiedDate = community.getModificationDate

  def getName = community.getName

  def getUniqueId = community.getDisplayName

  def getChildren = asJavaList(childCommunities.map(asResource) ++ childDocuments.map(asResource))

  def getCommunity = community

  def child(childName: String) = communityChild(childName) match {
    case c: Community => asResource(c)
    case d: Document => asResource(d)
    case _ => throw new IllegalArgumentException("unknown argument found for child: " + childName)
  }

  def createNew(name: String, inputStream: InputStream, length: Long, contentType: String) =
    childDocuments.find(d => d.getBinaryBody.getName == name) match {
      case Some(d) =>
      // Update the existing doc
        d.setBinaryBody(name, contentType, inputStream)
        d.save
        asResource(d)

      case None =>
      // No existing document was found, create a new one
        currentUser match {
          case Some(user) =>
            val dt = jc.getDocumentTypeManager.createDocumentType(name, name) // use the name as the description
            val doc: Document = jc.getDocumentManager.createDocument(user, dt, null, name, "")
            doc.setBinaryBody(name, contentType, inputStream)
            doc.save
            jc.getDocumentManager.addDocument(community, doc, null)
            new DocumentResource(doc, jc.getDocumentManager, sm)
          case None => throw new IllegalStateException("Cannot create a document with a user")
        }
    }


  def sendContent(out: OutputStream, range: Range, params: JMap[String, String], contentType: String) = {

    val w = new XmlWriter(out)
    w.open("html")
    w.open("body")
    w.begin("h1").open().writeText(this.getName).close
    w.open("table")
    asScalaIterable(getChildren).foreach{
      r =>
        w.open("tr")

        w.open("td")
        w.writeText(r.getName)
        w.close("td")

        w.begin("td").open().writeText(r.getModifiedDate() + "").close
        w.close("tr")
    }
    w.close("table")
    w.close("body")
    w.close("html")
    w.flush
  }


  def getContentType(accepts: String) = "text/html"

  protected def communityChild[T <: JiveObject](displayName: String): Option[T] = {

    def communityChildByDisplayName(displayName: String): Option[Community] =
      childCommunities.find{
        c1 => c1.getName == displayName
      }

    def documentChildByFileName(fileName: String): Option[Document] =
      childDocuments.find{
        d1 => d1.getBinaryBody.getName == fileName
      }

    communityChildByDisplayName(displayName) match {
      case Some(c) => Some(c.asInstanceOf[T])
      case None => documentChildByFileName(displayName) match {
        case Some(d) => Some(d.asInstanceOf[T])
        case None => None
      }
    }
  }

  protected def childCommunities = WebdavUtil.childCommunities(community, jc.getCommunityManager)

  protected def childDocuments = WebdavUtil.childDocuments(community, jc.getDocumentManager)

  protected def asResource(o: AnyRef): Resource = o match {
    case c: Community => new CommunityResource(c, jc, sm)
    case d: Document => new DocumentResource(d, jc.getDocumentManager, sm)
    case _ => throw new IllegalArgumentException("Unknown type " + o.getClass)
  }

  protected def jiveUrl = jc.getSpringBean("jiveProperties").asInstanceOf[JMap[String, String]].get("jiveURL")

}