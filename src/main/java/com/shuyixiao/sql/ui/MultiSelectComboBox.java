package com.shuyixiao.sql.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

/**
 * 支持多选的下拉复选框组件
 * 用于操作类型筛选（SELECT, INSERT, UPDATE, DELETE）
 */
public class MultiSelectComboBox extends JComboBox<MultiSelectComboBox.CheckBoxItem> {
    
    private final List<String> selectedItems = new ArrayList<>();
    private final List<ActionListener> changeListeners = new ArrayList<>();
    
    public MultiSelectComboBox(String[] items) {
        super();
        
        // 添加复选框项
        for (String item : items) {
            addItem(new CheckBoxItem(item, true)); // 默认全选
            selectedItems.add(item);
        }
        
        // 设置自定义渲染器
        setRenderer(new CheckBoxRenderer());
        
        // 添加事件监听
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                itemSelected();
            }
        });
    }
    
    /**
     * 处理项目选择事件
     */
    private void itemSelected() {
        CheckBoxItem item = (CheckBoxItem) getSelectedItem();
        if (item != null) {
            item.setSelected(!item.isSelected());
            
            // 更新选中项列表
            if (item.isSelected()) {
                if (!selectedItems.contains(item.getText())) {
                    selectedItems.add(item.getText());
                }
            } else {
                selectedItems.remove(item.getText());
            }
            
            // 通知监听器
            notifyChangeListeners();
        }
        
        // 保持下拉框打开状态
        setPopupVisible(true);
    }
    
    /**
     * 获取选中的项目
     */
    public List<String> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }
    
    /**
     * 设置选中的项目
     */
    public void setSelectedItems(List<String> items) {
        selectedItems.clear();
        selectedItems.addAll(items);
        
        // 更新复选框状态
        for (int i = 0; i < getItemCount(); i++) {
            CheckBoxItem item = getItemAt(i);
            item.setSelected(selectedItems.contains(item.getText()));
        }
        
        repaint();
        notifyChangeListeners();
    }
    
    /**
     * 全选
     */
    public void selectAll() {
        selectedItems.clear();
        for (int i = 0; i < getItemCount(); i++) {
            CheckBoxItem item = getItemAt(i);
            item.setSelected(true);
            selectedItems.add(item.getText());
        }
        repaint();
        notifyChangeListeners();
    }
    
    /**
     * 全不选
     */
    public void deselectAll() {
        selectedItems.clear();
        for (int i = 0; i < getItemCount(); i++) {
            CheckBoxItem item = getItemAt(i);
            item.setSelected(false);
        }
        repaint();
        notifyChangeListeners();
    }
    
    /**
     * 添加变化监听器
     */
    public void addChangeListener(ActionListener listener) {
        changeListeners.add(listener);
    }
    
    /**
     * 移除变化监听器
     */
    public void removeChangeListener(ActionListener listener) {
        changeListeners.remove(listener);
    }
    
    /**
     * 通知所有监听器
     */
    private void notifyChangeListeners() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "selectionChanged");
        for (ActionListener listener : changeListeners) {
            listener.actionPerformed(event);
        }
    }
    
    /**
     * 获取显示文本
     */
    public String getDisplayText() {
        if (selectedItems.isEmpty()) {
            return "无选择";
        } else if (selectedItems.size() == getItemCount()) {
            return "全部操作";
        } else {
            return String.join(", ", selectedItems);
        }
    }
    
    /**
     * 复选框项
     */
    public static class CheckBoxItem {
        private final String text;
        private boolean selected;
        
        public CheckBoxItem(String text, boolean selected) {
            this.text = text;
            this.selected = selected;
        }
        
        public String getText() {
            return text;
        }
        
        public boolean isSelected() {
            return selected;
        }
        
        public void setSelected(boolean selected) {
            this.selected = selected;
        }
        
        @Override
        public String toString() {
            return text;
        }
    }
    
    /**
     * 复选框渲染器
     */
    private class CheckBoxRenderer extends JCheckBox implements ListCellRenderer<CheckBoxItem> {
        
        public CheckBoxRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getListCellRendererComponent(
                JList<? extends CheckBoxItem> list,
                CheckBoxItem value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            
            if (value != null) {
                setText(value.getText());
                setSelected(value.isSelected());
                
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }
            }
            
            return this;
        }
    }
    
    /**
     * 自定义按钮显示
     */
    @Override
    public void setPopupVisible(boolean visible) {
        if (!visible) {
            // 当下拉框关闭时，更新按钮显示文本
            updateButtonText();
        }
        super.setPopupVisible(visible);
    }
    
    /**
     * 更新按钮文本
     */
    private void updateButtonText() {
        // 这里不需要做任何事，因为我们使用自定义的显示方式
    }
}

