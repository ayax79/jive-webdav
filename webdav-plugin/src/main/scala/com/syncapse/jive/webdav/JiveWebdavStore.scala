package com.syncapse.jive.webdav

import java.security.Principal
import java.io.InputStream
import java.lang.String
import net.sf.webdav.{ITransaction, IWebdavStore}
import com.jivesoftware.community.{DocumentManager, CommunityManager}
import com.syncapse.jive.Loggable

class JiveWebdavStore(communityMgr: CommunityManager, documentManager: DocumentManager) extends IWebdavStore with Loggable {
  def getStoredObject(transaction: ITransaction, uri: String) = {
    logger.info("JiveWebStore getStoredObject: " + uri)
    null
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
    null
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
}