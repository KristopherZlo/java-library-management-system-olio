package lms.ui;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import lms.report.CsvExporter;
import lms.report.ReportService;
import lms.service.LibraryService;
import lms.util.AppConfig;

public class UiContext {
    private final MultiWindowTextGUI gui;
    private final LibraryService libraryService;
    private final ReportService reportService;
    private final CsvExporter csvExporter;
    private final AppConfig config;

    public UiContext(MultiWindowTextGUI gui,
                     LibraryService libraryService,
                     ReportService reportService,
                     CsvExporter csvExporter,
                     AppConfig config) {
        this.gui = gui;
        this.libraryService = libraryService;
        this.reportService = reportService;
        this.csvExporter = csvExporter;
        this.config = config;
    }

    public MultiWindowTextGUI getGui() {
        return gui;
    }

    public LibraryService getLibraryService() {
        return libraryService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public CsvExporter getCsvExporter() {
        return csvExporter;
    }

    public AppConfig getConfig() {
        return config;
    }
}