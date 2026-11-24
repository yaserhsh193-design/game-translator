package com.gametranslator.tv;

/** Self-signed certificate so the iPhone page can use the live camera (Safari needs HTTPS). */
final class TlsCert {
    private TlsCert() {
    }

    static final String PASSWORD = "gametranslator";

    static final String P12_BASE64 =
            "MIIJswIBAzCCCWwGCSqGSIb3DQEHAaCCCV0EgglZMIIJVTCCBWEGCSqGSIb3DQEHAaCCBVIEggVOMIIFSjCCBUYGCyqGSIb3DQEM" +
            "CgECoIIE+zCCBPcwKQYKKoZIhvcNAQwBAzAbBBTNPKTuDoLrUHIgAbgiReo795CbtAIDAMNQBIIEyNQ7lUtRyO333G2Nh7CCP94r" +
            "iQ0dSVPoklET7mFfVyHekQnaUE427Yj8YVHFRpqazmRuDzNVhfl6Egmw6jJKPNuU6Ayu+EsrR0Y1IDHEFu1eHD6C0QVgLSAfQ6ry" +
            "m5FtwWTze8GylUBgfMXTttpg3JQ89vABoMXTXSZwjgeA82LxfOq20tPhmKEV8xUqPjTPBlNxkGEK3wcVei5XhkVGizDhogXGI9gO" +
            "9/trunQ+tiUdC2i59OGNHh1OjutTeyBxFhwXuSDnNDkxVroKYSkpXANMxl3QTXawYpXipCL4+k/jKLo0632Xg8yAzC1qHPec8rAN" +
            "v1HQ6DLE/kJQcmfm/5aqO3+x2LcIJ4K+bQC2NlLQUFQ3bfnS3/rMkeNgs2JYUZ09D7wUlAXKuNIPDE1hs2+svZTUXykHUZhjOgCN" +
            "cF7s/3HoAvr5Exs0UwaDmkcTyKw8eTD21OZIoKDiffBnbqbzGk/2sJY0k93MGHCO0ZSCX4z5Immsxoo0f3r1TOxROInNRF4ZI4Ca" +
            "PBz9o6MZmR1wfXR0iW+SgzRQMuqJsktOjUUM4xiNIihgZnRfI/nW7+nrMqUFMv82BBFD08zR7/WBSiX20sPe+8c/6LTPS0upndgj" +
            "wY6bGjB/KHSSVEHGnB1XaU35BGfHTPSDxw72oLBrDSYzWjLl0J6wy4I8qn6PaF0I2ZY0vyUcN1T3aUdbzHPKIvpvdGwYE3mquT53" +
            "sQ+Tnk3sJeWLjPB8f5Z43uMcyyToqo3BnNIJnw2hszlXzXYwkhyuz3wk32PGR3+k0Lyem+7HGEHOC/K5XPX/TTiGfiG5VtIFYKlk" +
            "eStLEYzpB76kvjHI7fP7kq1e0CJaXIe2ULl+X8Hk1Kk5VLdR29M7PDDbgYwbp0WzeCpmnWWC69R05cJzcDiIVPXX5UnvgZVPw90h" +
            "1wTbECTLrgjzMSvqeCRc8hrwIoyXfq2K93XdWbV9ovdkA/eEmvPx1TsvreP/0FMb9LAiKQNB2qhMl6lBJ2DMH4FFNN8Be4wWXSrI" +
            "ETuXaPp9XSR3qOQGxm0Q+6Wzw1VuLhEtv8C0AmItU32lwxmWTxq921ePO2ipKf+i6Pwaf92be947tVNsDq92fF2V1BA5IUlXWmrn" +
            "/Fq23VhJsrPZAJhFqdGkTi/thcVpDIAlhaxP+ei8JaCjP++E9pGvW2GANtAK0yD68yw9yOLv2TR92ZJeJnMpRmFqJcfhbbvTR6xN" +
            "IpbeV/nx8C1y3FtAYNpWOS8J/a3biq3HEk9cfOrmKg5L0g5PRDHIQjqOnK2nXialYmcSTV6SPEZAkz0PstTCTiS7HXp2QcyZyISe" +
            "hanKEqCkWZThnISSoCjPqVk+u8L5hnEBpgvGDCrYhYJdkhH08/WzHEx1QlDSZFnlkGlHh88wTnBLp44ZEJzUFPiNjW8IjT+omIcf" +
            "hXnoD669s4WOt7OHbl/8QsnuW+walzA7IoNFZVeKfKbYIOXt9BfXHKeSjRar4W3Sw34vU5P6GwtUxUrRo906meiMURR/QLpqXOl3" +
            "jF2+51S8X2B7rnV6dVlYjYo0Ajc6fBTUINhF4zvkvnSJtltLAYrKvOCSEO1rYMflNpar1xHm/vft3U+l2PwAhXtaHl+YF5GKKoXr" +
            "L/OawnN6iDE4MBMGCSqGSIb3DQEJFDEGHgQAdAB2MCEGCSqGSIb3DQEJFTEUBBJUaW1lIDE3OTU1NDI5NjI1NzEwggPsBgkqhkiG" +
            "9w0BBwagggPdMIID2QIBADCCA9IGCSqGSIb3DQEHATApBgoqhkiG9w0BDAEGMBsEFLOAsztTPjBr5DRklMoXqd1ezXfGAgMAw1CA" +
            "ggOYwyB7FYNEbfEULtNtaXacc6vlNectcXR4f5bCYZvAw3vFeQwwBGepHJQU+cKeoCCq/IGgsfhgsKruZMjdMy5Cpjk7IzAE969P" +
            "P+f1UMl8Z0jLYhSSg9Twb5bYix2TzIUdtTP5FI3KCiyvUtRzVcOSYjzB+rRtGETZFGOMg04K7SCuD2s0zNJx/edxAMwp9wNwdA4y" +
            "RWPpv+S/kYtRSFtro7OaGTtHgtQcfY5apOlxfpoFD4FzxG7agwklw06nKh4qYQvYtkFB3nZCS3bO8M5QaBe7PFeoWcvcUB5fAAoT" +
            "EICxCzN9jhoekjHTu/udqmyG8JyjP2J/wR6PRixYjxqvh2433NiHiw78rNJ6CII/81/TkY6qaYf00jLbtk/Gg9LjEkf8aN9zH3Fb" +
            "gDreqDraLDocePvP6YkLAf3HIQyFctZPjqsscscLOYol0TJl6kDo83RSH1ksvKGNmYXTFerN/C0u9zY6ri2ySMJzFEVfoPiM2W5K" +
            "CEfHMeP/mW2Rh4hDEGOtW+sWDgRgFPgM/4nBMA0KLHFSPIDMRqsFxBd9xTcyM7J1T6qq29El+T1XeNr7Uyl814kp5oOtHEzj+f5O" +
            "ExhRjx1pGUSF4/iFxwOV5MDSTsDoizRUJ91p9cpEYueQi4s52P3iSNoU8udbRVNDZC9ixW6G3P8g0dr+UnjBPfO3XUDFOFbEfrPP" +
            "zzNko7bWYU2XPQETCsJKFOi3SSX0jV8xPh9twDKiQEs7+aJIb0dbbAFm/VDy0mm7YAOczYtYIweGRsXfKsqUOIS72YR2Y3Xo86dz" +
            "WZERRB/ysrE++vBwmteqYv7dLjpRvP2X2J7tV1tvV44GFPWrmqP576DQvRibzsXudlOGKegm8VzLpfldt9o6pMOACqMvY63a98vF" +
            "dDT5TiEhkHmQ0zP6r/ux0sxsZMfoKMTZvAZf8GFmOEAII8GbLtDxvjI+WcNgERBOexqa28i1sxcN0MgKDdzric8m7fDv8r6zKEAu" +
            "/2c3Ug0bdE3oUh0wQWwJr7SEaOVtB30A9f2FSdP4HBe48PISmn1OyfHtzIXK8VFiYxsBtseTQGUdLKFaKkPEsGLLwhzLz9OvMjRg" +
            "rlN7PfJdr7xgUjWEctI+T5ZbB3uoIc2MMKBy5dR0MaPMY+cBToQ42RW8eiwI1xHGXm2+AgJhflNqGQLpPo7sjYwHzK3vwiuK6KJH" +
            "l4Gdyyh7/S94vAyWTNEnAFcaLMMu1uAwPjAhMAkGBSsOAwIaBQAEFJK+HXusE7bi9xbYUv/bcIAqmTGXBBSn7f46umN0nbHOwMjv" +
            "IWUGPBiFpAIDAYag";
}
