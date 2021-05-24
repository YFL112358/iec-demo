package com.example.demo.iec.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class TreeNode {
    private String fc;
    private String parentName;
    private String label;
    private String showName;
    private List<TreeNode> children = new ArrayList<TreeNode>();

    public TreeNode(String label, String showName) {
        this.label = label;
        this.showName = showName;
    }
    public TreeNode() {
    }
    public void add(TreeNode node) {
        children.add(node);
    }
}
