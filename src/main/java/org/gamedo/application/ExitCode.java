package org.gamedo.application;

public enum ExitCode {

    /**
     * 通过{@link ApplicationComponentRegister}向{@link GamedoApplication}注册bean时，存在重复的class
     */
    DuplicateApplicationComponent(-1),
    ;


    public final int code;

    ExitCode(int code) {
        this.code = code;
    }
}
