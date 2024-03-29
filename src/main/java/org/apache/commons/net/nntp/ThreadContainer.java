package org.apache.commons.net.nntp;

/**
 * A placeholder utility class, used for constructing a tree of Threadables
 * Original implementation by Jamie Zawinski. 
 * See the Grendel source for more details <a href="http://lxr.mozilla.org/mozilla/source/grendel/sources/grendel/view/Threader.java#511">here</a>
 * Threadable objects
 * @author Rory Winston <rwinston@apache.org>
 */
class ThreadContainer {
    Threadable threadable;
    ThreadContainer parent;
    ThreadContainer prev;
    ThreadContainer next;
    ThreadContainer child;

    /**
     * 
     * @param container
     * @return true if child is under self's tree. Detects circular references
     */
    boolean findChild(ThreadContainer target) {
        if (child == null)
            return false;

        else if (child == target)
            return true;
        else
            return child.findChild(target);
    }

    // Copy the ThreadContainer tree structure down into the underlying Threadable objects
    // (Make the Threadable tree look like the ThreadContainer tree)
    // TODO convert this to an iterative function - this can blow the stack 
    // with very large Threadable trees
    void flush() {
        if (parent != null && threadable == null)
            throw new RuntimeException("no threadable in " + this.toString());

        parent = null;

        if (threadable != null)
            threadable.setChild(child == null ? null : child.threadable);

        if (child != null) {
            child.flush();
            child = null;
        }

        if (threadable != null)
            threadable.setNext(next == null ? null : next.threadable);

        if (next != null) {
            next.flush();
            next = null;
        }

        threadable = null;
    }

    /**
     * Reverse the entire set of children
     *
     */
    void reverseChildren() {
        if (child != null) {
            ThreadContainer kid, prev, rest;
            for (prev = null, kid = child, rest = kid.next;
                kid != null;
                prev = kid,
                    kid = rest,
                    rest = (rest == null ? null : rest.next))
                kid.next = prev;

            child = prev;

            // Do it for the kids 
            for (kid = child; kid != null; kid = kid.next)
                kid.reverseChildren();
        }
    }
}
