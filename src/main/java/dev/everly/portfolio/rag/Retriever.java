package dev.everly.portfolio.rag;

import java.util.List;

public interface Retriever {
	List<String> retrieve(String query, int topK);
}