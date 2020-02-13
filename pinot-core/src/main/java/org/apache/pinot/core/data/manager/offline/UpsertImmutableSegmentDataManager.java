/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.core.data.manager.offline;

import org.apache.pinot.common.config.TableNameBuilder;
import org.apache.pinot.common.utils.CommonConstants;
import org.apache.pinot.common.utils.LLCSegmentName;
import org.apache.pinot.core.data.manager.UpsertSegmentDataManager;
import org.apache.pinot.core.indexsegment.UpsertSegment;
import org.apache.pinot.core.indexsegment.immutable.ImmutableSegment;
import org.apache.pinot.core.segment.updater.SegmentUpdater;
import org.apache.pinot.grigio.common.storageProvider.UpdateLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class UpsertImmutableSegmentDataManager extends ImmutableSegmentDataManager implements UpsertSegmentDataManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(UpsertImmutableSegmentDataManager.class);
  private String _tableNameWithType;

  public UpsertImmutableSegmentDataManager(ImmutableSegment immutableSegment) throws IOException {
    super(immutableSegment);
    _tableNameWithType = TableNameBuilder.ensureTableNameWithType(immutableSegment.getSegmentMetadata().getTableName(),
        CommonConstants.Helix.TableType.REALTIME);
    initVirtualColumns();
  }

  @Override
  public void updateVirtualColumns(List<UpdateLogEntry> messages) {
    ((UpsertSegment) _immutableSegment).updateVirtualColumn(messages);
  }

  @Override
  public String getVirtualColumnInfo(long offset) {
    return ((UpsertSegment) _immutableSegment).getVirtualColumnInfo(offset);
  }

  @Override
  public void destroy() {
    SegmentUpdater.getInstance().removeSegmentDataManager(_tableNameWithType, getSegmentName(), this);
    super.destroy();
  }

  private void initVirtualColumns() throws IOException {
    // 1. add listener for update events
    // 2. load all existing messages
    // ensure the above orders so we can ensure all events are received by this data manager
    LOGGER.info("adding data manager to listener for segment {}", getSegmentName());
    SegmentUpdater.getInstance().addSegmentDataManager(_tableNameWithType, new LLCSegmentName(getSegmentName()), this);
    LOGGER.info("initializing virtual column for segment {}", getSegmentName());
    ((UpsertSegment) _immutableSegment).initVirtualColumn();
  }
}