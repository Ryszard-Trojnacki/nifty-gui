/*
 * Copyright (c) 2015, Nifty GUI Community 
 * All rights reserved. 
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are 
 * met: 
 * 
 *  * Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer. 
 *  * Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in the 
 *    documentation and/or other materials provided with the distribution. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.lessvoid.niftyinternal.render;

import de.lessvoid.niftyinternal.tree.InternalNiftyTree;
import de.lessvoid.niftyinternal.common.Statistics;
import de.lessvoid.niftyinternal.math.Mat4;
import de.lessvoid.niftyinternal.render.batch.BatchManager;
import de.lessvoid.niftyinternal.render.sync.TreeSync;
import de.lessvoid.niftyinternal.render.sync.TreeSyncRenderNodeFactory;
import de.lessvoid.niftyinternal.render.sync.TreeSyncRenderState;
import de.lessvoid.nifty.spi.NiftyRenderDevice;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Take a list of public NiftyNodes and render them.
 *
 * This class internally translates the list of NiftyNodes into a list of RenderNodes. This means there exists
 * a duplicate of the original scene graph as a list of render nodes.
 *
 * The main reason for this extra amount of work is to prepare for better multithreading support later. We'd like to
 * let the source NiftyNodes be able to be modified freely and only at the synchronizing step we need to freeze the
 * source tree while it is being translated into the render tree.
 *
 * Additionally this frees the original NiftyNode scene graph from render specifics that we only keep in the RenderNode
 * hierachy.
 *
 * @author void
 */
public class NiftyRenderer implements NiftyRendererMXBean {
  private final static Logger logger = Logger.getLogger(NiftyRenderer.class.getName());

  // the reference to the Statistics instance we'll update
  private final Statistics stats;

  // since we need to render stuff we'll keep the NiftyRenderDevice instance around
  private final NiftyRenderDevice renderDevice;

  // the helper classes to sync real nifty nodes with render nodes 
  private final TreeSync treeSync;
  private final TreeSyncRenderState treeSyncRenderState;

  // the list of root render nodes representing the real Nifty root nodes (and their hierarchy)
  private final List<RenderNode> renderNodes = new ArrayList<RenderNode>();

  // we keep this list around to not allocate a list each frame - it's being reused
  // FIXME private final List<InternalNiftyNode> internalConvertList = new ArrayList<InternalNiftyNode>();

  /**
   * Constructor.
   *
   * @param stats the Statistics instance to gather stats about rendering
   * @param renderDevice the NiftyRenderDevice used for rendering
   */
  public  NiftyRenderer(final Statistics stats, final NiftyRenderDevice renderDevice) {
    this.stats = stats;
    this.renderDevice = renderDevice;
    this.treeSync = new TreeSync(new TreeSyncRenderNodeFactory(renderDevice));
    this.treeSyncRenderState = new TreeSyncRenderState();

    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer(); 
      ObjectName name = new ObjectName("de.lessvoid.nifty:type=NiftyRenderer");
      if (mbs.isRegistered(name)) {
        logger.warning(name + " already registered");
      } else {
        mbs.registerMBean(this, name);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Render the given InternalNiftyTree. This class will translate the Nodes into RenderNodes and keep a list.
   * On each following call this list will be synchronized with the tree.
   *
   * @param tree the Nifty tree that should be rendered
   * @return true when any content of the new frame changed and false if nothing has been rendered (since the frame
   * looks exactly as the last one). 
   */
  public boolean render(final InternalNiftyTree tree) {
    if (!synchronize(tree, renderNodes)) {
      return false;
    }

    render();
    return true;
  }

  private boolean synchronize(final InternalNiftyTree sourceTree, final List<RenderNode> destinationNodes) {
    /*
    stats.startSynchronize();
    List<InternalNiftyNode> internal = toInternal(sourceNodes);
    boolean syncChanged = treeSync.synchronizeTree(internal, destinationNodes);
    int renderStateChanged = treeSyncRenderState.synchronize(internal, destinationNodes);
    sortNodes(destinationNodes);
    stats.stopSynchronize();
    return syncChanged || (renderStateChanged != 0);
    */
    // FIXME
    return false;
  }
/* FIXME
  private List<InternalNiftyNode> toInternal(final List<NiftyNode> sourceNodes) {
    NiftyNodeAccessor niftyNodeAccessor = NiftyNodeAccessor.getDefault();

    internalConvertList.clear();
    for (int i=0; i<sourceNodes.size(); i++) {
      internalConvertList.add(niftyNodeAccessor.getInternalNiftyNode(sourceNodes.get(i)));
    }
    return internalConvertList;
  }
*/
  private void sortNodes(final List<RenderNode> destinationNodes) {
    Collections.sort(destinationNodes, new RenderNodeComparator());
    for (int i=0; i<destinationNodes.size(); i++) {
      destinationNodes.get(i).sortChildren();
    }
  }

  private void render() {
    renderDevice.beginRender();

    BatchManager batchManager = new BatchManager();
    batchManager.begin();
    for (int i=0; i<renderNodes.size(); i++) {
      renderNodes.get(i).render(batchManager, renderDevice, new Mat4());
    }
    batchManager.end(renderDevice);

    renderDevice.endRender();
  }

  // MBean
  private final StringBuilder getStateInfo = new StringBuilder();

  @Override
  public List<String> getRenderTree() {
    List<String> result = new ArrayList<String>();
    for (int i=0; i<renderNodes.size(); i++) {
      getStateInfo.setLength(0);
      renderNodes.get(i).outputStateInfo(getStateInfo, "");
      result.add(getStateInfo.toString());
    }
    return result;
  }
}