package sample.remote.servers
import com.roundeights.hasher.Implicits._

/**
  * Created by Chaoren on 4/14/16.
  */
object utilities {

  val m = 160

  def toHash(s:String):BigInt = {
    return BigInt(s.sha1.bytes).mod(BigInt(2).pow(m))
  }


  def rangeTellerEqualLeft(start:BigInt, end:BigInt, point:BigInt): Boolean = {
    if (start <= end) {
      if (point >= start && point < end)
        return true
      else
        return false
    }
    else {
      if (point >= start || point < end)
        return true
      else
        return false
    }
  }

  def rangeTellerEqualRight(start:BigInt, end:BigInt, point:BigInt): Boolean = {
    if (start <= end) {
      if (point > start && point <= end)
        return true
      else
        return false
    }
    else {
      if (point > start || point <= end)
        return true
      else
        return false
    }
  }

}
