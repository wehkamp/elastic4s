package com.sksamuel.elastic4s.http.index

import com.sksamuel.elastic4s.http.values.RefreshPolicyHttpValue
import com.sksamuel.elastic4s.http.{HttpExecutable, ResponseHandler}
import com.sksamuel.elastic4s.indexes.{IndexContentBuilder, IndexDefinition, IndexShowImplicits}
import org.apache.http.entity.{ContentType, StringEntity}
import org.elasticsearch.client.{Response, RestClient}

import scala.concurrent.Future

trait IndexImplicits extends IndexShowImplicits {

  implicit object IndexHttpExecutable extends HttpExecutable[IndexDefinition, IndexResponse] {

    override def responseHandler: ResponseHandler[IndexResponse] = ResponseHandler.failure404

    override def execute(client: RestClient, request: IndexDefinition): Future[Response] = {

      val (method, endpoint) = request.id match {
        case Some(id) => "PUT" -> s"/${request.indexAndType.index}/${request.indexAndType.`type`}/$id"
        case None => "POST" -> s"/${request.indexAndType.index}/${request.indexAndType.`type`}"
      }

      val params = scala.collection.mutable.Map.empty[String, String]
      request.createOnly.foreach(createOnly =>
        if(createOnly) {
          params.put("op_type", "create")
        }
      )
      request.routing.foreach(params.put("routing", _))
      request.parent.foreach(params.put("parent", _))
      request.timeout.foreach(params.put("timeout", _))
      request.refresh.map(RefreshPolicyHttpValue.apply).foreach(params.put("refresh", _))
      request.version.map(_.toString).foreach(params.put("version", _))
      request.versionType.map(VersionTypeHttpString.apply).foreach(params.put("version_type", _))

      val body = IndexContentBuilder(request)
      val entity = new StringEntity(body.string, ContentType.APPLICATION_JSON)

      logger.debug(s"Endpoint=$endpoint")
      client.async(method, endpoint, params.toMap, entity)
    }
  }
}


