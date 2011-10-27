package org.ovirt.engine.core.common.businessentities;

import java.io.Serializable;

//C# TO JAVA CONVERTER TODO TASK: The interface type was changed to the closest equivalent Java type, but the methods implemented will need adjustment:
public class VmsComparerByDiskSize implements java.util.Comparator<VM>, Serializable {
    private static final long serialVersionUID = -3546735706731409532L;

    @Override
    public int compare(VM o1, VM o2) {
        return Compare(o1, o2);
    }

    public int Compare(VM x, VM y) {
        return (int) (x.getActualDiskWithSnapshotsSize() - y.getActualDiskWithSnapshotsSize());
    }

    public VmsComparerByDiskSize() {
    }
}
