package phonis.survival.networking;

import java.io.DataOutputStream;
import java.io.IOException;

public interface RTSerializable {

    void toBytes(DataOutputStream dos) throws IOException;

}
