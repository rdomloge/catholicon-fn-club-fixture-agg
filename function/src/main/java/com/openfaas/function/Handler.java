package com.openfaas.function;

import java.util.Map;
import java.util.Map.Entry;

import com.openfaas.model.IRequest;
import com.openfaas.model.IResponse;
import com.openfaas.model.Response;

public class Handler extends com.openfaas.model.AbstractHandler {

    public IResponse Handle(IRequest req) {
        
        StringBuilder sb = new StringBuilder("Query\n");
        Map<String, String> query = req.getQuery();
        
        for(Entry<String,String> entry : query.entrySet()) {
            sb.append(entry.getKey()+": "+entry.getValue());
            sb.append("\n");
        }
        
        Response res = new Response();
	    res.setBody(sb.toString());

	    return res;
    }
}
