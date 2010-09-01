package com.syncapse.jive.webdav

import java.security.Principal
import java.io.InputStream
import java.lang.String
import com.syncapse.jive.Loggable
import net.sf.webdav.{StoredObject, ITransaction, IWebdavStore}
import java.util.Date
import collection.JavaConversions
import com.jivesoftware.community.{Community, DocumentManager, CommunityManager}

class JiveWebdavStore(communityMgr: CommunityManager, documentManager: DocumentManager) extends IWebdavStore with Loggable {
  def getStoredObject(transaction: ITransaction, uri: String) = {
    logger.info("JiveWebStore getStoredObject: " + uri)
    uri match {
      case "/" => RootStoredObject.asInstanceOf[StoredObject]
      case "communities" => JiveWebdavUtils.buildStoredObject(rootCommunity)
      case "spaces" => RootStoredObject.asInstanceOf[StoredObject]
      case _ => null
    }
  }

  def removeObject(transaction: ITransaction, uri: String) = {
    logger.info("JiveWebStore removeObject: " + uri)
    null
  }

  def getResourceLength(transaction: ITransaction, path: String) = {
    logger.info("JiveWebStore getResourceLength: " + path)
    0L
  }

  def getChildrenNames(transaction: ITransaction, folderUri: String) = {
    logger.info("JiveWebStore getChildrenNames: " + folderUri)
    def communityNames(community: Community) = {
      val communities: Iterable[Community] = JavaConversions.asIterable(communityMgr.getCommunities(community))
      communities.map(c => c.getName).toArray[String]
    }

    folderUri match {
      case "/" => Array[String]("communities", "spaces")
      case "communities" => communityNames(rootCommunity)
      case _ => matchingCommunity(folderUri) match {
        case Some(x) => communityNames(x)
        case None => null
      }
    }
  }

  def setResourceContent(transaction: ITransaction, resourceUri: String, content: InputStream, contentType: String, characterEncoding: String) = {
    logger.info("JiveWebStore setResourceContent: " + resourceUri)
    0L
  }

  def getResourceContent(transaction: ITransaction, resourceUri: String) = {
    logger.info("JiveWebStore getResourceContent: " + resourceUri)
    null
  }

  def createResource(transaction: ITransaction, resourceUri: String) = {
    logger.info("JiveWebStore createResource: " + resourceUri)
    null
  }

  def createFolder(transaction: ITransaction, folderUri: String) = {
    logger.info("JiveWebStore createFolder: " + folderUri)
    null
  }

  def rollback(transaction: ITransaction) = {
    logger.info("rollback called")
  }

  def commit(transaction: ITransaction) = {
    logger.info("commit called")
  }

  def checkAuthentication(transaction: ITransaction) = {
    logger.info("checkAuthentication called")
  }

  def begin(principal: Principal) = {
    logger.info("begin called")
    null
  }

  protected def rootCommunity = communityMgr.getRootCommunity

  protected def matchingCommunity(name: String) = {
    // todo, caching or something more efficient here
    val communities: Iterable[Community] = JavaConversions.asIterable(communityMgr.getRecursiveCommunities(rootCommunity))
    communities.find(c => c.getName == name)
  }

  protected object RootStoredObject extends StoredObject {
    setFolder(true)
    setLastModified(new Date(0))
    setCreationDate(new Date(0))
  }

}