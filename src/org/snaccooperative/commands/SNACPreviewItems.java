package org.snaccooperative.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNACPreviewItems {

  static final Logger logger = LoggerFactory.getLogger("SNACPreviewItems");

  protected int _itemCount;
  protected List<String> _itemsPreview;

  protected SNACPreviewItems(int itemCount) {
    this._itemCount = itemCount;
    this._itemsPreview = new ArrayList<String>();
  }

  @JsonProperty("item_count")
  public int getItemCount() {
    return _itemCount;
  }

  @JsonProperty("items_preview")
  public List<String> getItemsPreview() {
    return _itemsPreview;
  }

  public void addPreviewItem(String preview) {
    _itemsPreview.add(preview);
  }

  @Override
  public String toString() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return super.toString();
    }
  }
}
