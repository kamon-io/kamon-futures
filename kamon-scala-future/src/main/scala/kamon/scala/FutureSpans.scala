package kamon.scala

import kamon.Kamon
import kamon.Kamon.withContextKey
import kamon.trace.Span
import kamon.util.CallingThreadExecutionContext

import scala.concurrent.Future

object FutureSpans {
  def withNewAsyncSpan[T](operationName: String, spanKind: String, component: String, tags: Map[String, String] = Map.empty)(code: Span ⇒ Future[T]): Future[T] = {
    val spanBuilder = Kamon.buildSpan(operationName)
      .withMetricTag("span.kind", spanKind)
      .withTag("component", component)
    val spanBuilderWithTags = tags.foldLeft(spanBuilder){case (s, (k,v)) => s.withTag(k,v)}

    val span = spanBuilderWithTags.start()

    withSpanAsync(span)(code(span))
  }

  def withSpanAsync[T](span: Span)(f: => Future[T]): Future[T] = {
    try {
      withContextKey(Span.ContextKey, span)(f).transform(
        s = response => {
          span.finish()
          response
        },
        f = error => {
          span.addError("error.object", error)
          span.finish()
          error
        })(CallingThreadExecutionContext)
    } catch {
      case t: Throwable =>
        span.addError(t.getMessage, t)
        span.finish()
        throw t

    }
  }
}
