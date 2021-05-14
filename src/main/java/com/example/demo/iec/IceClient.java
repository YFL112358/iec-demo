package com.example.demo.iec;

import com.beanit.iec61850bean.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.*;

@Slf4j
@Component
@RestController
public class IceClient {
    @GetMapping("getIceData")
    public Map<String, String> getIceData() {
        Map<String, String> result = new HashMap<>();
        try {
            ClientSap clientSap = new ClientSap();
            InetAddress address = InetAddress.getByName("localhost");
            ClientAssociation clientAssociation = clientSap.associate(address, 102, null, null);
            ServerModel serverModel = clientAssociation.retrieveModel();
            long startTIme = System.currentTimeMillis();
            foreachNodesGetData(result, clientAssociation, serverModel);
            log.info((System.currentTimeMillis() - startTIme) + "");
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
        subNodes.forEach(subNode -> {
            getLeafNodesData(result, clientAssociation, serverModel, parent, subNode);
        });
    }

    private void getLeafNodesData(Map<String, String> result, ClientAssociation clientAssociation, ServerModel serverModel, ModelNode parent, ModelNode subNode) {
        Collection<ModelNode> leafNodes = subNode.getChildren();
        leafNodes.forEach(leafNode -> {
            if (leafNode instanceof FcModelNode) {
                FcModelNode leafFcNode = (FcModelNode) leafNode;
                String nodeName = parent.getName() + "/" + subNode.getName() + "." + leafFcNode.getName();
                FcModelNode fcModelNode = (FcModelNode) serverModel.findModelNode(nodeName, leafFcNode.getFc());
                getRealData(clientAssociation, fcModelNode);
                getResultData(result, nodeName, fcModelNode);
            }
        });
    }

    private void getResultData(Map<String, String> result, String nodeName, FcModelNode fcModelNode) {
        List<BasicDataAttribute> attributes = fcModelNode.getBasicDataAttributes();
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
}
