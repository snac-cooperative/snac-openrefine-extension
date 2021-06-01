package org.snaccooperative.exporters;

import org.snaccooperative.commands.SNACAPIResponse;

public abstract class SNACUploadItem {
  public abstract String getPreviewText();

  public abstract int rowIndex();

  public abstract String toJSON();

  public abstract SNACAPIResponse performUpload(String url, String key);
}
