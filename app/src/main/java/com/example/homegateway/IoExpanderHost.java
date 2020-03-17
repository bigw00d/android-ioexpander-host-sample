package com.example.homegateway;

import java.util.ArrayList;
import java.util.List;

public class IoExpanderHost {
    public static final int FLOW_OK = 0;
    public static final int FLOW_NG = -1;
    Callback callback;
    Flow flow;
    FlowCallback flowCallback;
    private List<Flow> flowList = new ArrayList<>();
    int flowIndex;
    int flowSize;

    public IoExpanderHost(IoExpanderHost.Callback callback) {
        this.callback = callback;
        flow = null;
    }

    public void processRcvData(String data) {
        if (flow != null) {
            int len = flow.processRcvData(data);
            if(len > 0) {
                String sendData = flow.getSendData();
                callback.sendData(sendData);
            }
            int flowState = flow.getFlowState();
            if (flowState != Flow.FLOW_ONGOING) {
                int result = FLOW_OK;
                if (flowState != Flow.FLOW_END_OK) {
                    result = FLOW_NG;
                    flowIndex = flowSize; // stop flow
                }

                flowIndex++;
                if (flowIndex >= flowSize) {
                    // end flow
                    flow = null;
                    flowList.clear();
                    flowCallback.endFlow(result);
                }
                else {
                    // next flow item
                    flow = flowList.get(flowIndex);
                }
            }
        }
    }

    public void addCommandFlow(Flow flow) {
        flowList.add(flow);
    }

    public void startCommandFlow(IoExpanderHost.FlowCallback callback) {
        this.flowCallback = callback;
        flowIndex = 0;
        flowSize = flowList.size();
        flow = flowList.get(flowIndex);
    }

    public interface Callback {
        public void sendData(String msg);
    }

    public interface Flow {
        public static final int FLOW_ONGOING = 0;
        public static final int FLOW_END_NG = -1;
        public static final int FLOW_END_OK = 1;
        public int processRcvData(String data);
        public String getSendData();
        public int getFlowState();
    }

    public interface FlowCallback {
        public void endFlow(int result);
        public void handleGetData(String data);
    }

    public static class WriteI2C implements Flow {
        int step;
        String sendData;
        int state;
        public WriteI2C(String slaveAddr, String data) {
            step = 0;
            sendData = "";
            state = FLOW_ONGOING;
        }
        public int getFlowState() {
            return state;
        }
        public String getSendData() {
            return sendData;
        }
        public int processRcvData(String data) {
            int sendLen = 0;
            switch (step) {
                case 0:
                    step++;
                    break;
                case 1:
                    step = 0;
                    break;
                default:
                    state = FLOW_END_NG;
                    step = 0;
                    break;
            }
            return sendLen;
        }
    }

    public static class ReadI2C implements Flow {
        int step;
        String sendData;
        int state;
        public ReadI2C(String slaveAddr, int len) {
            step = 0;
            sendData = "";
            state = FLOW_ONGOING;
        }
        public int getFlowState() {
            return state;
        }
        public String getSendData() {
            return sendData;
        }
        public int processRcvData(String data) {
            int sendLen = 0;
            switch (step) {
                case 0:
                    step++;
                    break;
                case 1:
                    step = 0;
                    break;
                default:
                    state = FLOW_END_NG;
                    step = 0;
                    break;
            }
            return sendLen;
        }
    }
}
