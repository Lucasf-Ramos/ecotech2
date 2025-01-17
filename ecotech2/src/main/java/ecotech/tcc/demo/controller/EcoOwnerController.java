package ecotech.tcc.demo.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ecotech.tcc.demo.model.Local;
import ecotech.tcc.demo.model.Usuario;
import ecotech.tcc.demo.model.GrupoResiduos;
import ecotech.tcc.demo.repository.GrupoResiduosReopository;
import ecotech.tcc.demo.repository.LocalReopository;

@Controller
@RequestMapping("/ecotech/ecoponto")

//carregar o formulario de cadastro do cliente
public class EcoOwnerController {

	@Autowired
	private LocalReopository localRepository;
	@Autowired
	private GrupoResiduosReopository grupoRepository;

	private Local localAtual;
	public boolean LoginErrado = false;
	String errorMsgLogin;
	String errorMsgcriarConta;
	String foto;

	@GetMapping("/perfil")
	public String telaPerfil(Model model, Local local) {
		if (localAtual != null) {
			model.addAttribute("locais", localAtual); // puxa o id do usuario que esta logado na conta atualmente
			if (localAtual.getFoto() != null) {
				foto = Base64.getEncoder().encodeToString(localAtual.getFoto());
				model.addAttribute("foto", foto);
			}
			else {
				model.addAttribute("foto", null);
			}
			
			
			return "donoEcoponto/perfilDono";
		}
		return "donoEcoponto/login";

	}

	@GetMapping("/novo-local") // cria a conta
	public String novoProduto(Local local, Model model) {
		
		model.addAttribute("erroMsg", errorMsgcriarConta);
		List<GrupoResiduos> grupoResiduosList = grupoRepository.findAll();
		List<String> cidades = localRepository.findAllUniqueCities();
		
		
		model.addAttribute("grupoResiduosList", grupoResiduosList);
		model.addAttribute("cidades", cidades);
		model.addAttribute("locais", local);
		return "donoEcoponto/novolocal";
	}

	@PostMapping("/add-local")
	public String gravarNovoLocal(Local local, BindingResult result, Model model) {

	    Local localEmail = localRepository.findByEmail(local.getEmail());
	    Local localCnpj = localRepository.findByCnpj(local.getCnpj());
	    List<GrupoResiduos> grupoResiduosList = grupoRepository.findAll();
		List<String> cidades = localRepository.findAllUniqueCities();

	    if (localEmail != null) {
	        // O email já existe
	        model.addAttribute("erroMsg", "O email já está em uso");
	        
	        model.addAttribute("grupoResiduosList", grupoResiduosList);
			model.addAttribute("cidades", cidades);
			model.addAttribute("locais", local);
			
			
	        return "donoEcoponto/novolocal";
	    } else if (localCnpj != null) {
	        // O CNPJ já está em uso
	        model.addAttribute("erroMsg", "O CNPJ já está em uso");
	        
	        model.addAttribute("grupoResiduosList", grupoResiduosList);
			model.addAttribute("cidades", cidades);
			model.addAttribute("locais", local);
			
			
	        return "donoEcoponto/novolocal";
	    } else {
	        local.setStatusPonto(false);
	        localRepository.save(local);
	        return "redirect:/ecotech/ecoponto/login";
	    }
	}

	@GetMapping("/login")
	public String login(Local local, Model model) {
		LoginErrado = true;

		model.addAttribute("locais", local);
		model.addAttribute("problem", LoginErrado);
		model.addAttribute("erro", errorMsgLogin);
		return "donoEcoponto/login";
	}

	@PostMapping("/login")
	public String efetuarLogin(Local local, Model model) {

		String page = "redirect:/ecotech/ecoponto/login";
		Local localdb = localRepository.findByEmail(local.getEmail());

		if (localdb != null && localdb.getSenha().equals(local.getSenha()) && localdb.isStatusPonto()) {
			page = "redirect:/ecotech/ecoponto/perfil";

			localAtual = localRepository.findByEmail(local.getEmail());
			LoginErrado = false;

		} else if (localdb != null && localdb.getSenha().equals(local.getSenha()) && !localdb.isStatusPonto()) {
			errorMsgLogin = "Seu Ecoponto não está ativo no momento, ele pode ter sido inativado pela nossa equipe.";
		} else {
			LoginErrado = true;
			errorMsgLogin = "Login ou senha errados";
		}

		return page;
	}

	@GetMapping("/editar")
	public String showUpdateForm(ModelMap model) {

		Local local = localAtual;
		String resid = local.getGrupoResiduos().getNome();

		List<GrupoResiduos> grupoResiduosList = grupoRepository.findAll();
		List<String> cidades = localRepository.findAllUniqueCities();

		if (local.getFoto() != null) {
			foto = Base64.getEncoder().encodeToString(local.getFoto());
		}
		else {
			model.addAttribute("foto", null);
		}
		

		model.addAttribute("local", local);
		model.addAttribute("residuo", resid);
		model.addAttribute("grupoResiduosList", grupoResiduosList);
		model.addAttribute("cidades", cidades);
		model.addAttribute("foto", foto);

		return "donoEcoponto/editar";
	}

	@PostMapping("/update")
	public String atualizarLocal(Local local, BindingResult result, @RequestParam(value = "file", required = false) MultipartFile file) {

		//localAtual = local;

		if (result.hasErrors()) {
			local.setId((long) localAtual.getId());
			return "donoEcoponto/editar";
		}
		if (file != null && !file.getOriginalFilename().isEmpty()) {
			try {
				local.setFoto(file.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			localAtual.setFoto(null);
			
		}
		
		local.setStatusPonto(localAtual.isStatusPonto());
		localRepository.save(local);

		localAtual = local;

		return "redirect:/ecotech/ecoponto/perfil";
	}

	@GetMapping("/logout")
	public String logOut() {

		localAtual = null;
		foto = "";
		return "redirect:/ecotech/user/index";
	}
	@GetMapping("/excluir")
	public String excluir() {
		localAtual.setStatusPonto(false);
		
		localAtual.setNome(localAtual.getNome()+"(Local Excluido)");
		localAtual.setEmail("");
		localAtual.setSenha("");
		localAtual.setFoto(null);
		foto = "";
		localRepository.save(localAtual);
		
		localAtual = null;
		
		return "redirect:/ecotech/index";
	}
	

}
