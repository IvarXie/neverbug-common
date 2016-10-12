/* =======================================================
 * 金色家网络科技有限公司-技术中心
 * 日 期：2016-3-30 16:45
 * 作 者：li.jianqiu
 * 版 本：0.0.1
 * 描 述：TODO
 * ========================================================
 */
package com.jyall.swagger;
/**
 * Created by li.jianqiu on 2016-3-30.
 */


import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @ClassName: FeignClientContentServlet </p>
 * @Author: li.jianqiu</p>
 * @Date: 2016-3-30 16:45 </p>
 * @Version: 0.0.1</p>
 * @Since: JDK 1.8</p>
 * @See: TODO</p>
 */
@WebServlet(value = "/swagger", name = "swagger")
public class SwaggerServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.sendRedirect("/swagger/index.html");
	}
}
