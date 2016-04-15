package sample.remote.calculator
import com.roundeights.hasher.Implicits._

/**
  * Created by Chaoren on 4/14/16.
  */
object utilities {
  def toHash(s:String):BigInt = {
    return BigInt(s.sha1.bytes)
  }


  def rangeTeller(start:BigInt, end:BigInt, point:BigInt): Boolean = {
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

}
