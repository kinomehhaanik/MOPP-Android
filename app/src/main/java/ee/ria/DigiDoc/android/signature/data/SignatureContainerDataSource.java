package ee.ria.DigiDoc.android.signature.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.File;

import ee.ria.DigiDoc.android.document.data.Document;
import ee.ria.DigiDoc.android.utils.files.FileStream;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface SignatureContainerDataSource {

    Single<File> addContainer(ImmutableList<FileStream> fileStreams);

    Single<SignatureContainer> get(File containerFile);

    Completable addDocuments(File containerFile, ImmutableList<FileStream> documentStreams);

    Completable removeDocuments(File containerFile, ImmutableSet<Document> documents);

    Single<File> getDocumentFile(File containerFile, Document document);
}