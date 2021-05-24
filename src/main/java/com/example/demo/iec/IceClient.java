package com.example.demo.iec;

import com.beanit.iec61850bean.*;
import com.example.demo.iec.entity.RequestEntity;
import com.example.demo.iec.entity.TreeNode;
import com.example.demo.iec.util.TreeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RestController
public class IceClient {
    private static List<ModelNode> iceModels = null;

    @CrossOrigin(origins = "*", maxAge = 3600)
    @GetMapping("getIceData")
    public List<TreeNode> getIceData() {
        iceModels = new ArrayList<>();
        List<TreeNode> treeNodes = new ArrayList<>();
        try {
            ClientSap clientSap = new ClientSap();
            InetAddress address = InetAddress.getByName("localhost");
            ClientAssociation clientAssociation = clientSap.associate(address, 102, null, null);
            ServerModel serverModel = clientAssociation.retrieveModel();
            long startTime = System.currentTimeMillis();
            getAllModel(serverModel.getChildren());
            System.out.println(System.currentTimeMillis() - startTime);
            List<String> rootNames = serverModel.getChildren().stream().map(ModelNode::getName).collect(Collectors.toList());
            startTime = System.currentTimeMillis();
            treeNodes = getModelTree(iceModels, rootNames.get(0));
            System.out.println(System.currentTimeMillis() - startTime);
            return treeNodes;
        } catch (IOException | ServiceError e) {
            log.error("have error", e);
        }
        return treeNodes;
    }

    private void getAllModel(Collection<ModelNode> parentNodes) {
        parentNodes.forEach(parentNode -> {
            getAllModelNodes(parentNode);
        });
    }

    private void getAllModelNodes(ModelNode parentNode) {
        if (parentNode.getChildren() == null || parentNode.getChildren().isEmpty()) {
            return;
        }
        iceModels.addAll(parentNode.getChildren());
        parentNode.getChildren().forEach(tempParentNode -> {
            getAllModelNodes(tempParentNode);
        });
    }

    @CrossOrigin(origins = "*", maxAge = 3600)
    @PostMapping("getRealIceData")
    public List<RequestEntity> getIceData(@RequestBody List<RequestEntity> request) {
        List<RequestEntity> result = new ArrayList<>();
        try {
            ClientSap clientSap = new ClientSap();
            InetAddress address = InetAddress.getByName("localhost");
            ClientAssociation clientAssociation = clientSap.associate(address, 102, null, null);
            ServerModel serverModel = clientAssociation.retrieveModel();
            request.forEach(re -> {
                FcModelNode modelNode = (FcModelNode) serverModel.findModelNode(re.getObjectName(), Fc.fromString(re.getFc()));
                getRealData(clientAssociation, modelNode);
                List<BasicDataAttribute> attributes = modelNode.getBasicDataAttributes();
                attributes.forEach(attribute -> {
                    String valueString = attribute.getValueString();
                    RequestEntity requestEntity = new RequestEntity();
                    if (valueString == null && attribute instanceof BdaUnicodeString) {
                        BdaUnicodeString bdaUnicodeString = (BdaUnicodeString) attribute;
                        try {
                            requestEntity.setObjectName(getLabelString(attribute));
                            requestEntity.setObjectValue(new String(bdaUnicodeString.getValue(), "UTF-8"));
                            requestEntity.setFc(attribute.getFc().name());
                        } catch (UnsupportedEncodingException e) {
                            log.error("have UnsupportedEncodingException ", e);
                        }
                    } else {
                        requestEntity.setObjectName(getLabelString(attribute));
                        requestEntity.setObjectValue(valueString);
                        requestEntity.setFc(attribute.getFc().name());
                    }
                    result.add(requestEntity);
                });
            });
        } catch (IOException | ServiceError e) {
            log.error("have error", e);
        }
        return result;
    }

    private void foreachNodesGetData(Map<String, String> result, ClientAssociation clientAssociation, ServerModel serverModel) {
        Collection<ModelNode> parentsNodes = serverModel.getChildren();
        parentsNodes.forEach(parent -> {
            getSubNodesData(result, clientAssociation, serverModel, parent);
        });
    }

    private void getSubNodesData(Map<String, String> result, ClientAssociation clientAssociation, ServerModel serverModel, ModelNode parent) {
        Collection<ModelNode> subNodes = parent.getChildren();
        iceModels.addAll(subNodes);
        subNodes.forEach(subNode -> {
            getLeafNodesData(result, clientAssociation, serverModel, parent, subNode);
        });
    }

    private void getLeafNodesData(Map<String, String> result, ClientAssociation clientAssociation, ServerModel serverModel, ModelNode parent, ModelNode subNode) {
        Collection<ModelNode> leafNodes = subNode.getChildren();
        iceModels.addAll(leafNodes);
        leafNodes.forEach(leafNode -> {
            if (leafNode instanceof FcModelNode) {
                FcModelNode leafFcNode = (FcModelNode) leafNode;
                String nodeName = parent.getName() + "/" + subNode.getName() + "." + leafFcNode.getName();
                FcModelNode fcModelNode = (FcModelNode) serverModel.findModelNode(nodeName, leafFcNode.getFc());
                //getRealData(clientAssociation, fcModelNode);
                //getResultData(result, nodeName, fcModelNode);
                iceModels.addAll(fcModelNode.getChildren());
            }
        });
    }

    private void getResultData(Map<String, String> result, String nodeName, FcModelNode fcModelNode) {
        List<BasicDataAttribute> attributes = fcModelNode.getBasicDataAttributes();
        iceModels.addAll(fcModelNode.getChildren());
        attributes.forEach(attribute -> {
            String valueString = attribute.getValueString();
            if (valueString == null && attribute instanceof BdaUnicodeString) {
                BdaUnicodeString bdaUnicodeString = (BdaUnicodeString) attribute;
                try {
                    result.put(nodeName + "." + attribute.getName(), new String(bdaUnicodeString.getValue(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    log.error("have UnsupportedEncodingException ", e);
                }
            } else {
                result.put(nodeName + "." + attribute.getName(), valueString);
            }
        });
    }

    private void getRealData(ClientAssociation clientAssociation, FcModelNode fcModelNode) {
        try {
            clientAssociation.getDataValues(fcModelNode);
        } catch (ServiceError e) {
            log.error("have error", e);
        } catch (IOException e) {
            log.error("have error", e);
        }
    }

    public List<TreeNode> getModelTree(List<ModelNode> modelNodes, String name) {
        List<TreeNode> treeList = modelNodes.stream()
                .filter(modelNode -> !modelNode.getName().equals(modelNode.getParent().getName()))
                .map(modelNode -> {
                    TreeNode node = new TreeNode();
                    node.setParentName(getLabelString(modelNode.getParent()));
                    node.setShowName(modelNode.getName());
                    if (modelNode instanceof FcModelNode) {
                        node.setFc(((FcModelNode) modelNode).getFc().name());
                    }
                    String labelName = getLabelString(modelNode);
                    node.setLabel(labelName);
                    return node;
                }).collect(Collectors.toList());
        treeList.add(new TreeNode(name, name));
        return TreeUtil.bulid(treeList, name);
    }

    private String getLabelString(ModelNode modelNode) {
        ObjectReference objectReference = modelNode.getReference();
        int size = objectReference.size();
        String labelName = "";
        if (size == 1) {
            labelName = modelNode.getName();
        }
        if (size == 2) {
            labelName = objectReference.get(0) + "/" + objectReference.get(1);
        }
        if (size > 2) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(objectReference.get(0) + "/" + objectReference.get(1));
            List<String> strings = new ArrayList();
            objectReference.iterator().forEachRemaining(it -> strings.add(it));
            strings.subList(2, size).forEach(str -> {
                stringBuffer.append(".");
                stringBuffer.append(str);
            });
            labelName = stringBuffer.toString();
        }
        return labelName;
    }

    @CrossOrigin(origins = "*", maxAge = 3600)
    @PostMapping("updateIceData")
    public List<RequestEntity> updateIceData(@RequestBody List<RequestEntity> request) {
        try {
            ClientSap clientSap = new ClientSap();
            InetAddress address = InetAddress.getByName("localhost");
            ClientAssociation clientAssociation = clientSap.associate(address, 102, null, null);
            ServerModel serverModel = clientAssociation.retrieveModel();
            request.forEach(re -> {
                FcModelNode constructedDataAttribute = (FcModelNode) serverModel.findModelNode(re.getObjectName(), Fc.fromString(re.getFc()));
                Object value = re.getObjectValue();
                if (value instanceof Integer) {
                    ((BdaInt32) constructedDataAttribute).setValue((Integer) value);
                }
                if (value instanceof String) {
                    ((BdaUnicodeString) constructedDataAttribute).setValue(((String) value).getBytes());
                }
                try {
                    clientAssociation.setDataValues(constructedDataAttribute);
                } catch (ServiceError | IOException e) {
                    log.error("have error", e);
                }
            });
        } catch (IOException | ServiceError e) {
            log.error("have error", e);
        }
        return request;
    }
}
