package kamon.scala.instrumentation

import kamon.Kamon
import kamon.scala.FutureSpans
import kamon.testkit.{MetricInspection, Reconfigure, TestSpanReporter}
import kamon.trace.Span
import kamon.util.Registration
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class FutureSpansSpec extends WordSpec with Matchers with MetricInspection with Eventually
  with Reconfigure with BeforeAndAfterAll with OptionValues {

  "FutureSpans withNewAsyncSpan" should {
    "create and finish span" in {

      FutureSpans.withNewAsyncSpan("operationName", "client", "test"){_ =>
        Future{1}
      }

      eventually(timeout(2 seconds)){
        val span = reporter.nextSpan().value
        span.operationName shouldBe "operationName"
        span.tags should contain ("span.kind" -> Span.TagValue.String("client"))
        span.tags should contain ("component" -> Span.TagValue.String("test"))
      }
    }

    "create and finish span with error when Future.failed" in {

      FutureSpans.withNewAsyncSpan("operationName", "client", "test"){_ =>
        Future.failed(new Exception("boom"))
      }

      eventually(timeout(2 seconds)){
        val span = reporter.nextSpan().value
        span.operationName shouldBe "operationName"
        span.tags should contain ("error.object" -> Span.TagValue.String("boom"))

      }
    }

    "create and finish span with error when exception" in {

      assertThrows[Exception] {
        FutureSpans.withNewAsyncSpan("operationName", "client", "test") { _ =>
          throw new Exception("boom")
        }
      }

      eventually(timeout(2 seconds)){
        val span = reporter.nextSpan().value
        span.operationName shouldBe "operationName"
        span.tags should contain ("error.object" -> Span.TagValue.String("boom"))

      }
    }
  }

  @volatile var registration: Registration = _
  val reporter = new TestSpanReporter()

  override protected def beforeAll(): Unit = {
    enableFastSpanFlushing()
    sampleAlways()
    registration = Kamon.addReporter(reporter)
  }

  override protected def afterAll(): Unit = {
    registration.cancel()
  }

}
